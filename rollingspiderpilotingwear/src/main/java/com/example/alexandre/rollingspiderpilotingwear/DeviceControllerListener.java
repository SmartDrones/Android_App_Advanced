package com.example.alexandre.rollingspiderpilotingwear;

public interface DeviceControllerListener
{
    public void onDisconnect();
    public void onUpdateBattery(final byte percent);
}
