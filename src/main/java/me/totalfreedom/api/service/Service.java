package me.totalfreedom.api.service;

public interface Service {
    default int id()
    {
        return (hashCode() >> 8) & 1;
    }

    void onStart();

    void onStop();
}
