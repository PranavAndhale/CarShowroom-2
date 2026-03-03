package models;

public class Dealership {
    private int id;
    private String name;
    private String address;
    private String city;
    private double latitude;
    private double longitude;
    private String openingTime;
    private String closingTime;
    private String phone;
    private double rating;

    public Dealership() {}

    public Dealership(int id, String name, String address, String city,
                      double latitude, double longitude, String openingTime,
                      String closingTime, String phone, double rating) {
        this.id = id; this.name = name; this.address = address; this.city = city;
        this.latitude = latitude; this.longitude = longitude;
        this.openingTime = openingTime; this.closingTime = closingTime;
        this.phone = phone; this.rating = rating;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }
    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public String getTimings() { return openingTime + " – " + closingTime; }

    @Override
    public String toString() { return name + " | " + city + " | " + getTimings(); }
}
