package com.kautiainen.antti.dunerest.db;

/**
 * Motivation identifier combines person identifier and motivaiton identifier.
 */
public class MotivationId {

  private Integer personId = null;

  private Integer motivationId = null;

  public MotivationId() {}

  public MotivationId(Integer personId, Integer motivationId)
    throws IllegalArgumentException {
    setPersonId(personId);
    setMotivationId(motivationId);
  }

  public Integer getPersonId() {
    return personId;
  }

  public void setPersonId(Integer personId) {
    if (this.personId == null) {
      if (personId == null) throw new IllegalArgumentException(
        "Invalid person identifier"
      );
      this.personId = personId;
    } else {
      throw new UnsupportedOperationException("Person identifier is immutable");
    }
  }

  public Integer getMotivationId() {
    return motivationId;
  }

  public void setMotivationId(Integer motivationId) {
    if (this.motivationId == null) {
      if (motivationId == null) throw new IllegalArgumentException(
        "Invalid motivation identifier"
      );
      this.motivationId = motivationId;
    } else {
      throw new UnsupportedOperationException(
        "Motivaton identifier is immutable"
      );
    }
  }
}
