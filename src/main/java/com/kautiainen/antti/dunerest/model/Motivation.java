package com.kautiainen.antti.dunerest.model;

/**
 * A motivation of a person.
 */
public class Motivation extends NamedAndValuedObject<Integer> {

  /**
   * Set the current drive statement of the motivation.
   * @return The current drive statement, or an udefined value if the
   * motivatio has none.
   */
  public String getStatement() {
    return statement;
  }

  /**
   * Set the current drive statement of the motivation.
   * @param statement The new drive statement.
   */
  public void setStatement(String statement) {
    this.statement = statement;
  }

  /**
   * The current drive statement of the value.
   */
  private String statement;

  /**
   * Is the motivation challenged, and thus not available.
   */
  private boolean isChallenged = false;

  /**
   * Create a new motivation with a name, a value, and a drive statement.
   * @param name The name of the motivation.
   * @param value The value of the motivation.
   * @param statement The drive statement of the motivation.
   * @throws IllegalArgumentException Any parameter was invalid.
   */
  public Motivation(String name, int value, String statement)
    throws IllegalArgumentException {
    super(name, value);
    setStatement(statement);
  }

  /**
   * Create a new motivation with a name, and a value, but without a drive
   * statement.
   * @param name The name of the created motivation.
   * @param value The value of the created motivation.
   * @throws IllegalArgumentException Any parameter was invalid.
   */
  public Motivation(String name, int value) throws IllegalArgumentException {
    this(name, value, null);
  }

  /**
   * Create a new motivation with a name, and a default value, but without a drive
   * statement.
   * @param name The name of the created motivation.
   * @throws IllegalArgumentException Any parameter was invalid.
   */
  public Motivation(String name) throws IllegalArgumentException {
    this(name, defaultValue());
  }

  /**
   * Test the validity of the value.
   * @param value The tested value.
   * @return True, if and only if the value is valid.
   */
  public boolean validValue(int value) {
    return value <= maximumValue() && value >= minimumValue();
  }

  /**
   * Get the default value.
   * @return The default motivation value.
   */
  public static int defaultValue() {
    return minimumValue();
  }

  /**
   * Get the mimimum value of the motivation.
   * @return The smallest allowed value of a motivaiton.
   */
  public static int minimumValue() {
    return 4;
  }

  /**
   * Get the maximum value of the motivation.
   * @return The largest allowed value of a motivation.
   */
  public static int maximumValue() {
    return 8;
  }

  /**
   * Is the motivation challenged.
   * @return True, if and only if the motivation is challenged.
   */
  public synchronized boolean isChallenged() {
    return isChallenged;
  }

  /**
   * Set whether the motivation is challenged or not.
   * @param isChallenged Will the statement be challenged after this command.
   */
  public synchronized void setChallenged(boolean isChallenged) {
    this.isChallenged = isChallenged;
  }

  /**
   * Is the current motivation available.
   * @return
   */
  public boolean isAvailable() {
    return !isChallenged;
  }

  /**
   * Motivation controller altering the current motivation.
   */
  public class MotivationController {

    /**
     * The message indicating the motivation is not challenged.
     */
    public static final String THE_MOTIVATION_IS_NOT_CHALLENGED_MESSAGE =
      "The motivation is not challenged";

    /**
     * Chanllenge the statement making the motivation unavailable.
     */
    public synchronized void challengeStatement() {
      synchronized (Motivation.this) {
        setChallenged(true);
      }
    }

    /**
     * Resolve a challenged motivation.
     * @param improved The motivation improved.
     * @throws IllegalArgumentException The improved motivation was either undefined, or the motivation was the
     * current motivation.
     * @throws InvalidChangeException The change of the curretn motivation or the improved motivation was not possible.
     * @throws IllegalStateException The motivation was not challenged, and thus resolving the conflict
     * is not possible.
     */
    public synchronized void resolveChallengedStatement(Motivation improved)
      throws IllegalArgumentException, IllegalStateException {
      Motivation motivation = Motivation.this;
      if (improved == null) {
        throw new IllegalArgumentException(
          "Cannot resolve challenge by improving an undefined motivation"
        );
      }

      // Performing the change in the synchronized block ensuring only we
      // alter the motivations involved.
      synchronized (motivation) {
        if (!motivation.isChallenged()) {
          throw new IllegalStateException(
            THE_MOTIVATION_IS_NOT_CHALLENGED_MESSAGE
          );
        }

        synchronized (improved) {
          if (java.util.Objects.equals(motivation, improved)) {
            throw new IllegalArgumentException(
              "Cannot resolve challenge by improving the challenged motivation"
            );
          }

          if (motivation.getValue() == Motivation.minimumValue()) {
            throw new InvalidChangeException(
              "Cannot reduce motivation below minimum"
            );
          }
          if (improved.getValue() >= Motivation.maximumValue()) {
            throw new InvalidChangeException(
              "Cannot increase motivation above maximum"
            );
          }
          improved.setValue(improved.getValue() + 1);
          motivation.setValue(Motivation.this.getValue() - 1);
        }
      }
    }

    /**
     * Resolve a challenged motivation.
     * @param statement The new drive statement resolving the conflict.
     * @throws IllegalArgumentException The given drive statement was either undefined, or the
     * drive statement the motitvaion.
     * @throws IllegalStateException The motivation was not challenged, and thus resolving the conflict
     * is not possible.
     */
    public synchronized void resolveChallengedStatement(String statement)
      throws IllegalArgumentException, IllegalStateException {
      synchronized (Motivation.this) {
        if (isChallenged()) {
          if (
            statement == null ||
            java.util.Objects.equals(statement, Motivation.this.statement)
          ) {
            // The drive statmeent must change, as the motivation values does nto change.
            throw new IllegalArgumentException(
              "The drive statement must change"
            );
          } else {
            // Change the drive statement.
            setStatement(statement);
            setChallenged(false);
          }
        } else {
          // The motivation is not challenged.
          throw new IllegalStateException(
            THE_MOTIVATION_IS_NOT_CHALLENGED_MESSAGE
          );
        }
      }
    }
  }
}
