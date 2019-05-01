package org.sakaiproject.progress.api;

/**
 * Stores the role of a user. Created by Jonathon Ross 1/23/2019
 */
public enum ProgressRole {

    //Stores the different value mappings

    STUDENT("section.role.student"),
    TA("section.role.ta"),
    INSTRUCTOR("section.role.instructor"),
    NONE("section.role.none");

    private String value;

    /**
     * Stores the value of the user's role
     * @param value
     */
    ProgressRole (final String value) {
        this.value = value;
    }

    /**
     * Get the actual name of the role
     *
     * @return String with the user's role
     */
    public String getValue() {
        return this.value;
    }
}

