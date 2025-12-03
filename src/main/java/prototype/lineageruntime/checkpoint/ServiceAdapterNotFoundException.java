package prototype.lineageruntime.checkpoint;

public class ServiceAdapterNotFoundException extends RuntimeException {

    public ServiceAdapterNotFoundException(String serviceId) {
        super("No state adapter registered for service " + serviceId);
    }
}


