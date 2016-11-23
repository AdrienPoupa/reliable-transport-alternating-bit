package com.ReliableTransportStarter.SelectiveRepeat;

/**
 * Created by Yigang on 10/27/2015.
 */
public interface EventList
{
    public boolean add(Event e);
    public Event removeNext();
    public String toString();
    public Event removeTimer(int entity);
    public double getLastPacketTime(int entityTo);
}