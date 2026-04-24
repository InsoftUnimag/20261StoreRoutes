package co.edu.unimagdalena.storelogistic.consultar.domain.models;

public class Carrier {

    private final Long carrierId;
    private final String name;
    private final String status;
    private final String email;

    public Carrier(Long carrierId, String name, String status, String email) {
        this.carrierId = carrierId;
        this.name      = name;
        this.status    = status;
        this.email     = email;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    public Long carrierId() { return carrierId; }
    public String name()    { return name; }
    public String status()  { return status; }
    public String email()   { return email; }
}
