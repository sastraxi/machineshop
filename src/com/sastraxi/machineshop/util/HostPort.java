package com.sastraxi.machineshop.util;

public class HostPort {

    private final String host;
    private final int port;
    
    public HostPort(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
    
    public String toString() {
        return host + ":" + port;
    }
    
    public static HostPort fromString(String s) {
        return HostPort.fromString(s, 22);
    }
    
    public static HostPort fromString(String s, int defaultPort) {
        String[] parts = s.split(":");
        if (parts.length == 1) {
            return new HostPort(s, defaultPort);
        } else {
            return new HostPort(parts[0], Integer.parseInt(parts[1]));
        }
    }
    
}
