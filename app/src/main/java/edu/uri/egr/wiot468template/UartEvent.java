package edu.uri.egr.wiot468template;

public class UartEvent {
    public byte type;
    public int data;

    public UartEvent(byte type, int data) {
        this.type = type;
        this.data = data;
    }
}
