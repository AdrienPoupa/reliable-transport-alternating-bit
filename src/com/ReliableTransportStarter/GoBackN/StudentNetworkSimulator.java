package com.ReliableTransportStarter.GoBackN;

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

	private int astate = 0;
	private int bstate = 0;
	private int bOnceThru = 0;
	private Packet astored_packet;
	private Packet bstored_packet;

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
		LimitSeqNo = winsize+1;
		RxmtInterval = delay;
	}

    // window buffer for tracking sent packets
	private Packet[] buffer;
    
    // double list for storing communication times
	private LinkedList<Double> rtt = new LinkedList<Double>();
    // double array for tracking sent and ack times
	private double[] tsent;
    private double[] tackd;
    // int var for tracking next seq # A will send to B
    private int nextSequence = FirstSeqNo;
    // int var for keeping track of base of the sending window
    private int base = 1;
    // int var for tracking next seq # B expects to rcv
    private int eseq = 1;
    // init new packet for impending ack transmission later
	private Packet acknowledgedPacket = new Packet(0, 0, 0);
	// int var for tracking num packets sent
    private int sent = 0; 
    // int var tracking num retransmissions
    private int retrans = 0;
    // int var tracking num packets received 
    private int received = 0;
	// int var tracking num corrupted packets received
    private int numCorruptedPackets = 0;
    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
    	// Store data from message received in String var for use in payload later
    	String received = message.getData();
    	if(nextSequence < base + WindowSize) {
		    // Compute checksum for following packet creation
		 	int checksum = calculateChecksum(nextSequence,-1,received);
			Packet packet = new Packet(nextSequence,-1,checksum,received);
			
			// Print String representation of Packet
	    	System.out.println("aOutput received: " + packet.toString());
	    	
			// If we reached end of array...
			if(nextSequence > WindowSize) {
				System.out.println("Wrapping Around");
				// Wrap around
				buffer[nextSequence %LimitSeqNo] = packet;
			}
			
			// Else, just add new packet to sending buffer (will be removed once acknowledgement received)		
			else { 
				buffer[nextSequence] = packet;
			}
			
			// Send Packet packet to layer 3 of B
			toLayer3(A, packet);
			
			// Record start time and add to list
			long time1 = System.nanoTime();
			
			System.out.println("timesent: " + time1);
			tsent[nextSequence] = time1;
			
			// Increment sent counter
			sent++;
			
			System.out.println("aOutput snt: " + packet.toString());
			
			// If base of window gets slid over but nextSequence is not updated
			if (base== nextSequence) {
				// Start timeout for A for time specified by delay inputted by user triggering a timeout in which lost packets are retransmitted
				startTimer(A, RxmtInterval);
			}
			
			// Increase nextseqnum
			nextSequence++;
		}
    }
 
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {	
    	// Record acknowledgement time and add to list
		double time2 = System.nanoTime();
		System.out.println("timeakd: "+ time2);
		
		tackd[nextSequence] = time2;
    	// Increase received counter
    	received++;
    	
    	// Make copy of Packet 'packet' and store as Packet
    	Packet p = new Packet(packet);
    	
    	// Print String representation of Packet
    	System.out.println("aInput: " + p.toString());
    	
    	// Store fields of the packet
    	int seq = p.getSeqnum(); int ack = p.getAcknum(); int chks = p.getChecksum();
    	
    	// Calculate what checksum should be and store in var 'chk'
    	int chk = seq+ack;
    	
    	// Compare this calculated checksum with that of the packet received
    	// If it is not corrupted
    	if (chks == chk) {
    		// Update base
    		base = p.getAcknum() + 1;
    		
    		// If we have slid the base of our window over to the next seq #...
    		if (base== nextSequence) {
    			// Stop timer, bc we are where we want to be
    			stopTimer(A);
    		}
    		
    		// Otherwise, trigger timeout in which we will retransmit not acknowledged packets
    		else {
    			startTimer(A, RxmtInterval);
			}
    	}
    	// Increment corrupt counter
    	else { 
    		numCorruptedPackets++; 
    	}
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
    	// Start timer for retransmission
    	startTimer(A, RxmtInterval);
    	
    	// Resend all packets previously sent but not yet acknowledged (waiting in send buffer)
    	for(int i = base; i< nextSequence; i++) {
    		// if we are past window...
    		if(i > WindowSize) {
    			// Print String representation of Packet 
    	    	System.out.println("aTimerInterrupt (Wrap): " + buffer[i%LimitSeqNo].toString());
    	    	
    	    	// Print seqnum
    	    	System.out.println("nextSequence = " + nextSequence);
    	    	toLayer3(A, buffer[i % LimitSeqNo]);
    	    	
    	    	// Increment sent & retransmission counter
    	    	sent++; retrans++;
    		}
    		
    		else { 
    			// Print String representation of packet
    	    	System.out.println("aTimerInterrupt: " + buffer[i].toString());
    			toLayer3(A, buffer[i]);

    			// Increment sent & retransmission counter
    	    	sent++; retrans++;
			}
    	}
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
    	// initialize window buffer to WindowSize+1, to allow for wrap around
		buffer = new Packet[LimitSeqNo];
		tsent = new double[1050];
		tackd = new double[1050];
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {	// Increment received count
    	received++;

    	// Make copy of Packet 'packet' and store as packet
    	Packet p = new Packet(packet);

    	// Print String representation of packet
    	System.out.println("bInput received: " + p.toString());

    	// Print seqnum
    	System.out.println("nextSequence = " + nextSequence);

    	// Print seqnum expected
    	System.out.println("eseq = " + eseq);

    	// Store fields of packet
    	int seq = p.getSeqnum(); int ack = p.getAcknum();
    	int chks = p.getChecksum();
    	String pay = p.getPayload();

    	// Calculate what checksum should be and store in var 'check'
    	int check = calculateChecksum(seq,ack,pay);

    	// Print this calc'd checksum
    	System.out.println("checksum should be: " + check);

    	// Declare var for storing ack checksum
    	int ackCheck;

    	// Compare calc'd checksum with that of the packet received, as well as the seq #
    	// if seq no. and checksum are what we expect...
    	if ((seq == eseq) && (chks == check)) {
    		// Send payload to layer5
    		toLayer5(pay);

    		// Generate ack # for ack packet
    		ack = seq;

    		// Generate new checksum for ack packet
    		ackCheck = seq+ack;

    		// Update ack packet
    		acknowledgedPacket = new Packet(eseq,ack,ackCheck);

    		// Print str of acknowledgedPacket
    		System.out.println("new acknowledgedPacket: " + acknowledgedPacket.toString());

    		// Send ack to layer3
    		toLayer3(B, acknowledgedPacket);

    		// Increase sent count and next expected sequence number
    		sent++;
    		eseq++;
    		System.out.println("new eseq: " + eseq);
    	}
    	else{
    		if (chks != check) {
    			// Increment corruption count
    			numCorruptedPackets++;
    		}
    		// If no last packet received (first msg lost), return
    		if(acknowledgedPacket.getSeqnum() <= 0) {
    			return;
    		}
    		else {
	    		// Else, print str of last acknowledgedPacket and send to layer3
	    		System.out.println("last acknowledgedPacket: " + acknowledgedPacket.toString());
	    		toLayer3(B, acknowledgedPacket);
    		}
    	}
    		
    }

	// This routine will be called once, before any of your other B-side
	// routines are called. It can be used to do any required
	// initialization (e.g. of member variables you add to control the state
	// of entity B).
	protected void bInit()
	{
		// You probably won't need to put any code in this method.  It is from another assignment.
	}

	// Translate String 'str' into an int, and add this to seq and ack to generate a checksum
	// then return that checksum
	protected int calculateChecksum(int seq, int ack, String str) {

		// Generate int representation of our payload
		int pay = str.hashCode();
		
		return seq + ack + pay;
	}

    // Use to print final statistics
    protected void simulationFinished()
    {
		int numlost = sent - received;
    	double sum = 0;
    	double artt;
    	
    	// Add all rtt's to rtt list
    	for(int i = 0; i < tsent.length - 1; i++) {
    		// Compute difference in acknowledgement and trans time
    		rtt.add(i, tackd[i]-tsent[i]);

    		// Sum the list
    		sum += rtt.get(i);
    	}
    	
    	// Divide sum by number of times to get average rtt
    	artt = sum / rtt.size();
    	System.out.println("sent: " + sent);
    	System.out.println("retransmitted: " + retrans);
    	System.out.println("received: " + received);
    	System.out.println("lost: " + numlost);
    	System.out.println("corrupt: " + numCorruptedPackets);
    	System.out.println("artt: " + artt + " ms");
    	
    }	

}
