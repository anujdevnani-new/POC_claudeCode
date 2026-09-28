package ai.agilery.example;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Custom DateFormat for parsing and formatting dates according to the RFC 3339
 * standard.
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 *
 * <p>
 * Note: This class is final to prevent subclassing.
 * </p>
 */
public final class RFC3339DateFormat extends DateFormat {

    /**
     * Serialization identifier for this class. This value is used during the
     * deserialization process to ensure that a loaded class corresponds exactly to
     * the serialized object.
     * <p>
     * The value is set to 1L, which is a standard convention for initial
     * versioning.
     * </p>
     */
    private static final long serialVersionUID = 1L;

    /**
     * The time zone used for date and time operations in this class.
     * <p>
     * This is set to "UTC" to ensure consistent time zone handling across different
     * environments, eliminating issues related to local time zone variations.
     * </p>
     */
    private static final TimeZone TIMEZONE_Z = TimeZone.getTimeZone("UTC");

    /**
     * Date format instance used for parsing and formatting dates.
     * <p>
     * This instance of {@link StdDateFormat} is configured with the UTC time zone
     * and includes a colon in the time zone offset. This ensures that dates are
     * formatted and parsed with a consistent time zone representation.
     * </p>
     */
    private final StdDateFormat fmt = new StdDateFormat().withTimeZone(TIMEZONE_Z).withColonInTimeZone(true);

    /**
     * Constructs a new RFC3339DateFormat instance.
     */
    public RFC3339DateFormat() {
        super();
        this.calendar = new GregorianCalendar();
        this.numberFormat = NumberFormat.getInstance();
    }

    /**
     * Parses a date from the given string starting at the specified position.
     *
     * @param source the string to parse
     * @param pos    the position to start parsing from
     * @return the parsed date, or null if the date could not be parsed
     */
    @Override
    public Date parse(final String source, final ParsePosition pos) {
        return fmt.parse(source, pos);
    }

    /**
     * Formats the given date and appends the formatted date to the given string
     * buffer.
     *
     * @param date          the date to format
     * @param toAppendTo    the string buffer to append the formatted date to
     * @param fieldPosition the field position (unused)
     * @return the string buffer with the formatted date appended
     */
    @Override
    public StringBuffer format(final Date date, final StringBuffer toAppendTo, final FieldPosition fieldPosition) {
        return fmt.format(date, toAppendTo, fieldPosition);
    }
}
