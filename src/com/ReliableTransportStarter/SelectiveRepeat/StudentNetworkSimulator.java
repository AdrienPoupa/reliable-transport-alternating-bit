package com.ReliableTransportStarter.SelectiveRepeat;

import java.util.*;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity):
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment):
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData):
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   PLEASE USE THE FOLLOWING VARIABLES IN YOUR ROUTINES AS APPROPRIATE.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.

    private int aApplication = 0;
    private int aTransition = 0;
    private int bApplication = 0;
    private int bTransition = 0;

    // Current packet for each entity
    private Packet packetFromA;
    private Packet packetFromB;

    // For Sender
    private int head;
    private int tail;
    private int sndBase;
    private int nextSeqNum;
    private ACKpkt[] senderWindow;
    private ArrayList<Packet> sendBuffer;
    private int timeoutCount;

    // For Receiver
    private int rcvBase;
    private Packet[] rcvBuffer;

    // ACKed packets number
    private int ACKed = 0;

    // Variables for counting loss and corruption packets and rate
    private int corrupt = 0;

    // Variables for counting average retransmission time
    private int retNum = 0;

    // Timer
    private ArrayList<Timer> timerLine = new ArrayList<>();

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = winsize+ 1;
        RxmtInterval = delay;
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message) {
        aApplication++;
        int checksum = 0;

        // Set packet
        packetFromA.setSeqnum(nextSeqNum);
        packetFromA.setAcknum(0);
        packetFromA.setPayload(message.getData());

        checksum += nextSeqNum;
        checksum += packetFromA.getAcknum();
        for (int i = 0; i < packetFromA.getPayload().length(); i++)
            checksum += (int) packetFromA.getPayload().charAt(i);

        packetFromA.setChecksum(checksum);

        tail++;
        sendBuffer.add(packetFromA);

        int temp = nextSeqNum- sndBase +head;

        while(nextSeqNum < sndBase + WindowSize && temp != tail+ 1)
        {
            System.out.println("temp: " + temp);
            senderWindow[(nextSeqNum- 1) % WindowSize].setSeqNum((sendBuffer.get(temp)).getSeqnum());
            senderWindow[(nextSeqNum- 1) % WindowSize].setAck(false);
            toLayer3(A, sendBuffer.get(temp));

            aTransition++;

            if(sndBase == nextSeqNum)
            {
                startTimer(A, RxmtInterval);
                Timer timer = new Timer();
                timerLine.add(timer);
                timer.setSendRTT(getTime());
                timer.setSendCom(getTime());
                timer.setSequenceNumber(nextSeqNum);
            }

            nextSeqNum++;
            temp++;
        }
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        int checksum;
        checksum = packet.getSeqnum();
        checksum += packet.getAcknum();
        for(int i = 0; i < packet.getPayload().length(); i++)
            checksum += (int)packet.getPayload().charAt(i);

        if(packet.getChecksum() == checksum)
        {
            if (packet.getAcknum() == sndBase)
            {
                int iter = sndBase + 1;
                timeoutCount = 0;
                head++;

                while (senderWindow[(iter - 1) % WindowSize].getSeqNum() != - 1 && senderWindow[(iter - 1) % WindowSize].getAck())
                {
                    senderWindow[(iter - 1) % WindowSize].setSeqNum(- 1);
                    senderWindow[(iter - 1) % WindowSize].setAck(false);
                    iter++;
                    head++;
                }
                sndBase = iter;

                if (sndBase == nextSeqNum)
                {
                    stopTimer(A);

                    ACKed++;

                    for(Timer e : timerLine)
                    {
                        if(e.getSequenceNumber() == packet.getAcknum())
                        {
                            e.setReceiveCom(getTime());
                            e.setReceiveRTT(getTime());
                        }
                    }
                }
                else
                {
                    stopTimer(A);
                    startTimer(A, RxmtInterval);
                }
            }
            else if(packet.getAcknum() > sndBase && packet.getAcknum() < nextSeqNum)
            {
                senderWindow[(packet.getAcknum() - 1) % WindowSize].setSeqNum(packet.getAcknum());
                senderWindow[(packet.getAcknum() - 1) % WindowSize].setAck(true);
            }
        }
        else
        {
            corrupt++;
            System.out.println("A: Corrupt packet received.");
        }
    }

    // This routine will be called when A's timer expires (thus generating a
    // timer interrupt). You'll probably want to use this routine to control
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped.
    protected void aTimerInterrupt()
    {
        aTransition++;

        toLayer3(A, sendBuffer.get(head));
        startTimer(A, RxmtInterval);

        for(Timer e : timerLine)
        {
            if(e.getSequenceNumber() == sendBuffer.get(head).getSeqnum())
            {
                e.setSendRTT(getTime());
            }
        }

        timeoutCount++;

        int temp = head + 1;
        for(int i = 1; i < timeoutCount && i < WindowSize; i++)
        {
            //New line maybe not correct to work
            if(temp == sendBuffer.size())
                break;

            if(senderWindow[(sndBase +i- 1) % WindowSize].getSeqNum() != - 1 && !senderWindow[(sndBase +i- 1) % WindowSize].getAck())
            {
                toLayer3(A, sendBuffer.get(temp));

                retNum++;

                aTransition++;

                for(Timer e : timerLine)
                {
                    if(e.getSequenceNumber() == sendBuffer.get(temp).getSeqnum())
                    {
                        e.setSendRTT(getTime());
                    }
                }
            }
            temp++;
        }
    }

    // This routine will be called once, before any of your other A-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        head = FirstSeqNo;
        tail = - 1;
        sndBase = 1;
        nextSeqNum = 1;
        sendBuffer = new ArrayList<>();
        packetFromA = new Packet(0, 0, 0, null);

        senderWindow = new ACKpkt[WindowSize];
        for(int i = 0; i < WindowSize; i++) {
            senderWindow[i] = new ACKpkt();
        }
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        bTransition++;

        int checksum;
        checksum = packet.getSeqnum();
        checksum += packet.getAcknum();
        for(int i = 0; i < packet.getPayload().length(); i++)
            checksum += (int)packet.getPayload().charAt(i);

        if(packet.getChecksum() != checksum)
        {
            corrupt++;
            System.out.println("B: Corrupt packet received.");
            return;
        }


        if(packet.getSeqnum() == rcvBase)
        {
            toLayer5(packet.getPayload());

            bApplication++;

            int iter = rcvBase + 1, rcvBaseIncrease = 1;
            while(rcvBuffer[(iter- 1) % WindowSize].getSeqnum() != - 1)
            {
                toLayer5(rcvBuffer[(iter- 1) % WindowSize].getPayload());

                bApplication++;

                rcvBaseIncrease++;
                rcvBuffer[(iter- 1) % WindowSize].setSeqnum(- 1);
                iter++;
            }
            rcvBase += rcvBaseIncrease;
        }
        else if(packet.getSeqnum() > rcvBase && packet.getSeqnum() < rcvBase + WindowSize)
        {
            int index = (packet.getSeqnum()- 1) % WindowSize;

            if(rcvBuffer[index].getSeqnum() != packet.getSeqnum())
                rcvBuffer[index] = packet;
        }
        else if(packet.getSeqnum() >= rcvBase - WindowSize && packet.getSeqnum() < rcvBase)
        {
            System.out.println("B: Drop this received packet for out of window.");
        }
        else
        {
            System.out.println("B: Invalid packet received.");
            return;
        }

        packetFromB.setSeqnum(0);
        packetFromB.setAcknum(packet.getSeqnum());
        packetFromB.setPayload(null);
        packetFromB.setChecksum(packet.getSeqnum());

        toLayer3(B, packetFromB);
    }

    // This routine will be called once, before any of your other B-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {

        rcvBase = 1;
        packetFromB = new Packet(0, 0, 0, null);
        rcvBuffer = new Packet[WindowSize];
        for(int i = 0; i < WindowSize; i++)
            rcvBuffer[i] = new Packet(- 1, 0, 0, null);
    }

    // Use to print final statistics
    protected void simulationFinished()
    {
        double totaltime = getTime();

        double sumRtt = 0;
        double sumCom = 0;

        for(Timer e : timerLine)
        {
            if(e.getReceiveCom() != 0 && e.getReceiveRTT() != 0)
            {
                double rtt = e.getReceiveRTT() - e.getSendRTT();
                double com = e.getReceiveCom() - e.getSendCom();
                sumRtt += rtt;
                sumCom += com;
            }
        }
        double avgRTT = sumRtt / timerLine.size();
        double avgComTime = sumCom / timerLine.size();

        System.out.println();
        System.out.println("Simulation finished.");
        System.out.println("Protocol: SR result:");

        System.out.println(aApplication + " packets sent from application layer of the sender.");
        System.out.println(aTransition + " packets sent from the transport layer of the sender.");
        System.out.println(bTransition + " packets received at the transport layer of receiver.");
        System.out.println(bApplication + " packets received at the application layer of the receiver.");

        System.out.println("Total time cost: " + totaltime);

        System.out.println("Average RTT: " + avgRTT);
        System.out.println("Average communication time: " + avgComTime);

        System.out.println("ACKnowledged packets: " + ACKed);

        System.out.println("Retransmission time: " + retNum);
        System.out.println("Corrupt packet number: " + corrupt);

        System.out.println("Throughput: " + bTransition / totaltime);
        System.out.println("Goodput: " + bApplication / totaltime);

    }
}
