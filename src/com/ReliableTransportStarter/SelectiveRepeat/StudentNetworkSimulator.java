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
    private int sendBase;
    private int nextSequenceNumber;
    private AcknowledgedPacket[] senderWindow;
    private ArrayList<Packet> sendBuffer;
    private int timeoutCount;

    // For Receiver
    private int receiveBase;
    private Packet[] rcvBuffer;

    // acknowledged packets number
    private int acknowledged = 0;

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
        packetFromA.setSeqnum(nextSequenceNumber);
        packetFromA.setAcknum(0);
        packetFromA.setPayload(message.getData());

        checksum += nextSequenceNumber;
        checksum += packetFromA.getAcknum();
        for (int i = 0; i < packetFromA.getPayload().length(); i++)
            checksum += (int) packetFromA.getPayload().charAt(i);

        packetFromA.setChecksum(checksum);

        tail++;
        sendBuffer.add(packetFromA);

        int temp = nextSequenceNumber - sendBase + head;

        // Send the message if necessary
        while(nextSequenceNumber < sendBase + WindowSize && temp != tail + 1)
        {
            System.out.println("temp: " + temp);
            senderWindow[(nextSequenceNumber - 1) % WindowSize].setSeqNum((sendBuffer.get(temp)).getSeqnum());
            senderWindow[(nextSequenceNumber - 1) % WindowSize].setAck(false);

            // Send packet to layer 3
            toLayer3(A, sendBuffer.get(temp));

            aTransition++;

            if(sendBase == nextSequenceNumber)
            {
                startTimer(A, RxmtInterval);
                // Start timer
                Timer timer = new Timer();
                timerLine.add(timer);
                timer.setSendRTT(getTime());
                timer.setSendCom(getTime());
                timer.setSequenceNumber(nextSequenceNumber);
            }

            nextSequenceNumber++;
            temp++;
        }
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        // Compute checksum
        int checksum;
        checksum = packet.getSeqnum();
        checksum += packet.getAcknum();
        for(int i = 0; i < packet.getPayload().length(); i++)
            checksum += (int)packet.getPayload().charAt(i);

        // Packet has to be dealt with
        if(packet.getChecksum() == checksum)
        {
            // First acknowledgement
            if (packet.getAcknum() == sendBase)
            {
                int iter = sendBase + 1;
                timeoutCount = 0;
                head++;

                while (senderWindow[(iter - 1) % WindowSize].getSeqNum() != - 1 && senderWindow[(iter - 1) % WindowSize].getAck())
                {
                    senderWindow[(iter - 1) % WindowSize].setSeqNum(- 1);
                    senderWindow[(iter - 1) % WindowSize].setAck(false);
                    iter++;
                    head++;
                }
                sendBase = iter;

                // Reading complete
                if (sendBase == nextSequenceNumber)
                {
                    // Finally stop timer
                    stopTimer(A);

                    acknowledged++;

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
                    // Stop current timer
                    stopTimer(A);

                    // Start a new one
                    startTimer(A, RxmtInterval);
                }
            }
            // Set packet ready for treatment
            else if(packet.getAcknum() > sendBase && packet.getAcknum() < nextSequenceNumber)
            {
                senderWindow[(packet.getAcknum() - 1) % WindowSize].setSeqNum(packet.getAcknum());
                senderWindow[(packet.getAcknum() - 1) % WindowSize].setAck(true);
            }
        }
        // Packet is corrupt
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

        // Send to layer 3
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
            // New line maybe not correct to work
            if(temp == sendBuffer.size())
                break;

            if(senderWindow[(sendBase + i - 1) % WindowSize].getSeqNum() != - 1 && !senderWindow[(sendBase + i - 1) % WindowSize].getAck())
            {
                // Send to layer 3
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
        sendBase = 1;
        nextSequenceNumber = 1;
        sendBuffer = new ArrayList<>();
        packetFromA = new Packet(0, 0, 0, null);

        senderWindow = new AcknowledgedPacket[WindowSize];
        for(int i = 0; i < WindowSize; i++) {
            senderWindow[i] = new AcknowledgedPacket();
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

        // If packet has to be dealt with
        if(packet.getSeqnum() == receiveBase)
        {
            // Send to layer 5
            toLayer5(packet.getPayload());

            bApplication++;

            int iter = receiveBase + 1;
            int receiveBaseIncrease = 1;
            while(rcvBuffer[(iter- 1) % WindowSize].getSeqnum() != - 1)
            {
                toLayer5(rcvBuffer[(iter- 1) % WindowSize].getPayload());

                bApplication++;

                receiveBaseIncrease++;
                rcvBuffer[(iter- 1) % WindowSize].setSeqnum(- 1);
                iter++;
            }
            receiveBase += receiveBaseIncrease;
        }
        // Set packet ready for treatment
        else if(packet.getSeqnum() > receiveBase && packet.getSeqnum() < receiveBase + WindowSize)
        {
            int index = (packet.getSeqnum()- 1) % WindowSize;

            if(rcvBuffer[index].getSeqnum() != packet.getSeqnum())
                rcvBuffer[index] = packet;
        }
        // If packet is out of window
        else if(packet.getSeqnum() >= receiveBase - WindowSize && packet.getSeqnum() < receiveBase)
        {
            System.out.println("B: Drop this received packet for out of window.");
        }
        // Otherwise, unknown error
        else
        {
            System.out.println("B: Invalid packet received.");
            return;
        }

        // Send final packet to layer 3
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
        receiveBase = 1;
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

        System.out.println(aApplication + " packets sent from application layer of the sender.");
        System.out.println(aTransition + " packets sent from the transport layer of the sender.");
        System.out.println(bTransition + " packets received at the transport layer of receiver.");
        System.out.println(bApplication + " packets received at the application layer of the receiver.");

        System.out.println("Total time cost: " + totaltime);

        System.out.println("Average RTT: " + avgRTT);
        System.out.println("Average communication time: " + avgComTime);

        System.out.println("Acknowledged packets: " + acknowledged);

        System.out.println("Retransmission time: " + retNum);
        System.out.println("Corrupt packet number: " + corrupt);

        System.out.println("Throughput: " + bTransition / totaltime);
        System.out.println("Goodput: " + bApplication / totaltime);

    }
}
