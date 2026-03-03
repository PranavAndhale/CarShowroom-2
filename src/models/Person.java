package models;

public abstract class Person {
    protected int id;
    protected String name;
    protected String email;
    protected String phone;
    protected String address;

    public Person() {}

    public Person(int id, String name, String email, String phone, String address) {
        this.id = id; this.name = name; this.email = email;
        this.phone = phone; this.address = address;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public abstract String getRole();

    @Override
    public String toString() { return name + " (" + getRole() + ")"; }
}
