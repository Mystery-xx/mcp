package io.github.solas.mcp.weather.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Nominatim reverse geocoding API.
 */
class NominatimReverseResponse {

    @JsonProperty("place_id")
    private String placeId;

    @JsonProperty("licence")
    private String licence;

    @JsonProperty("osm_type")
    private String osmType;

    @JsonProperty("osm_id")
    private String osmId;

    @JsonProperty("lat")
    private String lat;

    @JsonProperty("lon")
    private String lon;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("address")
    private Address address;

    @JsonProperty("category")
    private String category;

    @JsonProperty("type")
    private String type;

    @JsonProperty("importance")
    private Double importance;

    public NominatimReverseResponse() {
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public String getOsmType() {
        return osmType;
    }

    public void setOsmType(String osmType) {
        this.osmType = osmType;
    }

    public String getOsmId() {
        return osmId;
    }

    public void setOsmId(String osmId) {
        this.osmId = osmId;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getImportance() {
        return importance;
    }

    public void setImportance(Double importance) {
        this.importance = importance;
    }

    /**
     * Address components from reverse geocoding.
     */
    static class Address {
        @JsonProperty("house_number")
        private String houseNumber;

        @JsonProperty("road")
        private String road;

        @JsonProperty("neighbourhood")
        private String neighbourhood;

        @JsonProperty("suburb")
        private String suburb;

        @JsonProperty("city")
        private String city;

        @JsonProperty("town")
        private String town;

        @JsonProperty("village")
        private String village;

        @JsonProperty("county")
        private String county;

        @JsonProperty("state")
        private String state;

        @JsonProperty("postcode")
        private String postcode;

        @JsonProperty("country")
        private String country;

        @JsonProperty("country_code")
        private String countryCode;

        public Address() {
        }

        public String getHouseNumber() {
            return houseNumber;
        }

        public void setHouseNumber(String houseNumber) {
            this.houseNumber = houseNumber;
        }

        public String getRoad() {
            return road;
        }

        public void setRoad(String road) {
            this.road = road;
        }

        public String getNeighbourhood() {
            return neighbourhood;
        }

        public void setNeighbourhood(String neighbourhood) {
            this.neighbourhood = neighbourhood;
        }

        public String getSuburb() {
            return suburb;
        }

        public void setSuburb(String suburb) {
            this.suburb = suburb;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getTown() {
            return town;
        }

        public void setTown(String town) {
            this.town = town;
        }

        public String getVillage() {
            return village;
        }

        public void setVillage(String village) {
            this.village = village;
        }

        public String getCounty() {
            return county;
        }

        public void setCounty(String county) {
            this.county = county;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getPostcode() {
            return postcode;
        }

        public void setPostcode(String postcode) {
            this.postcode = postcode;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
    }
}
