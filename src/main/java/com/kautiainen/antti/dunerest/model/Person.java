package com.kautiainen.antti.dunerest.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The person of the dune.
 */
public class Person {

  private String name;

  /**
   * Traits of the character. Multiple traits are combined
   * to a single trait.
   */
  private Set<Trait> traits = new HashSet<>();

  /**
   * The skills of the character.
   */
  private Set<Skill> skills = new HashSet<>();

  /**
   * The taletns of the character.
   */
  private Set<Talent> talents = new HashSet<>();

  /**
   * Motivations of the character.
   */
  private Set<Motivation> motivations = new HashSet<>();

  /**
   * Assets of the character.
   */
  private List<Assets> assets = new ArrayList<>();

  public Person() {}

  /**
   * Set the name of the person.
   * @param name The name of the person.
   * @throws IllegalArgumentException The name was invalid.
   */
  public void setName(String name) throws IllegalArgumentException {
    if (validName(name)) {
      this.name = name;
    }
  }

  /**
   * Test validity of the name.
   * @param name The tested name.
   * @return True, if and only if the name is acceptable.
   */
  public boolean validName(String name) {
    return name != null && !name.isBlank() && name.trim().equals(name);
  }

  public String getName() {
    return this.name;
  }

  public Set<Trait> getTraits() {
    return traits;
  }

  public void setTraits(Set<Trait> traits) {
    this.traits = traits;
  }

  public Set<Skill> getSkills() {
    return skills;
  }

  public void setSkills(Set<Skill> skills) {
    this.skills = skills;
  }

  public Set<Talent> getTalents() {
    return talents;
  }

  public void setTalents(Set<Talent> talents) {
    this.talents = talents;
  }

  public Set<Motivation> getMotivations() {
    return motivations;
  }

  public void setMotivations(Set<Motivation> motivations) {
    this.motivations = motivations;
  }

  /**
   * Get the assets of the character.
   * @return An always existing list of the character assets.
   * @implNote The changes on the list are reflected to the assets of the
   * character, and changes on the assets of the reflects to the returned
   * list.
   */
  public synchronized List<Assets> getAssets() {
    return assets;
  }

  /**
   * Set the assets of the character.
   * @param assets The new assets of the character.
   */
  public synchronized void setAssets(List<Assets> assets) {
    // Demanding access on both assets list.
    synchronized (this.assets) {
      synchronized (assets) {
        List<Assets> backup = new ArrayList<>(this.assets);
        try {
          this.assets.clear();
        } catch (Exception e) {
          throw new UnsupportedOperationException(
            "Setting of the assets not supported"
          );
        }
        try {
          this.assets.addAll(assets);
        } catch (
          IllegalArgumentException
          | ClassCastException
          | NullPointerException exception
        ) {
          // Reverting the assts back.
          this.assets.addAll(backup);
          throw new IllegalArgumentException("Invalid new assets", exception);
        }
      }
    }
    this.assets = assets;
  }

  public static Object getId() {
    return null;
  }
}
