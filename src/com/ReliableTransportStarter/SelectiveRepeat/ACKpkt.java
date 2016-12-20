package com.ReliableTransportStarter.SelectiveRepeat;

public class ACKpkt
{
    private int seqNum = -1;
    private boolean acked = false;

    public ACKpkt(){}

    public int getSeqNum()
        {
            return this.seqNum;
        }

    public boolean getAck()
        {
            return this.acked;
        }

    public void setSeqNum(int s)
        {
            this.seqNum = s;
        }

    public void setAck(boolean a)
        {
            this.acked = a;
        }

}
