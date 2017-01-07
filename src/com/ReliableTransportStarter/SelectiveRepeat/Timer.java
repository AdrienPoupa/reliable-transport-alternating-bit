package com.ReliableTransportStarter.SelectiveRepeat;

public class Timer
{
    private double sendRTT = 0;
    private double receiveRTT = 0;
    private double sendCom = 0;
    private double receiveCom = 0;
    private int sequenceNumber = -1;

    public double getSendRTT()
    {
        return this.sendRTT;
    }

    public double getReceiveRTT()
    {
        return this.receiveRTT;
    }

    public double getSendCom()
    {
        return this.sendCom;
    }

    public double getReceiveCom()
    {
        return this.receiveCom;
    }

    public int getSequenceNumber()
    {
        return this.sequenceNumber;
    }

    public void setSendRTT(double sendRTT)
    {
        this.sendRTT = sendRTT;
    }

    public void setReceiveRTT(double receiveRtt)
    {
        this.receiveRTT = receiveRtt;
    }

    public void setSendCom(double sendCom)
    {
        this.sendCom = sendCom;
    }

    public void setReceiveCom(double receiveCom)
    {
        this.receiveCom = receiveCom;
    }

    public void setSequenceNumber(int sequenceNumber)
    {
        this.sequenceNumber = sequenceNumber;
    }
}
