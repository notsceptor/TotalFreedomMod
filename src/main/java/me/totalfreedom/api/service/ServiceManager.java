package me.totalfreedom.api.service;

import java.util.HashSet;
import java.util.Set;

public final class ServiceManager 
{
    private final Set<Service> services = new HashSet<>();

    public ServiceManager()
    {
        services.stream()
                .filter(s -> s != null)
                .forEach(Service::onStart);
    }

    public void registerService(final Service service)
    {
        services.add(service);
    }

    public void dropService(final Service service)
    {
        services.remove(service);
    }
}
