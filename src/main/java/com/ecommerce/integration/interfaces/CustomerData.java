package com.ecommerce.integration.interfaces;

/**
 * Customer data for payment and billing operations.
 * Contains customer information needed for boleto generation and payment processing.
 */
public class CustomerData {
    
    private final String name;
    private final String email;
    private final String document;
    private final String documentType;
    private final String phone;
    private final AddressData address;
    
    private CustomerData(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.document = builder.document;
        this.documentType = builder.documentType;
        this.phone = builder.phone;
        this.address = builder.address;
    }
    
    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDocument() { return document; }
    public String getDocumentType() { return documentType; }
    public String getPhone() { return phone; }
    public AddressData getAddress() { return address; }
    
    public static class Builder {
        private String name;
        private String email;
        private String document;
        private String documentType = "CPF";
        private String phone;
        private AddressData address;
        
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder document(String document) { this.document = document; return this; }
        public Builder documentType(String documentType) { this.documentType = documentType; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder address(AddressData address) { this.address = address; return this; }
        
        public CustomerData build() {
            return new CustomerData(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class AddressData {
        private final String street;
        private final String number;
        private final String complement;
        private final String neighborhood;
        private final String city;
        private final String state;
        private final String zipCode;
        
        private AddressData(AddressBuilder builder) {
            this.street = builder.street;
            this.number = builder.number;
            this.complement = builder.complement;
            this.neighborhood = builder.neighborhood;
            this.city = builder.city;
            this.state = builder.state;
            this.zipCode = builder.zipCode;
        }
        
        // Getters
        public String getStreet() { return street; }
        public String getNumber() { return number; }
        public String getComplement() { return complement; }
        public String getNeighborhood() { return neighborhood; }
        public String getCity() { return city; }
        public String getState() { return state; }
        public String getZipCode() { return zipCode; }
        
        public static class AddressBuilder {
            private String street;
            private String number;
            private String complement;
            private String neighborhood;
            private String city;
            private String state;
            private String zipCode;
            
            public AddressBuilder street(String street) { this.street = street; return this; }
            public AddressBuilder number(String number) { this.number = number; return this; }
            public AddressBuilder complement(String complement) { this.complement = complement; return this; }
            public AddressBuilder neighborhood(String neighborhood) { this.neighborhood = neighborhood; return this; }
            public AddressBuilder city(String city) { this.city = city; return this; }
            public AddressBuilder state(String state) { this.state = state; return this; }
            public AddressBuilder zipCode(String zipCode) { this.zipCode = zipCode; return this; }
            
            public AddressData build() {
                return new AddressData(this);
            }
        }
        
        public static AddressBuilder builder() {
            return new AddressBuilder();
        }
    }
}