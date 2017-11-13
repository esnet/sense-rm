package net.es.nsi.cs.lib;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Helper {

  private final static String URN_UUID = "urn:uuid:";

  public static String getUUID() {
    return URN_UUID + UUID.randomUUID().toString();
  }

  /**
   * Computes the day that has the given offset in days to today and returns it as an instance of <code>Calendar</code>.
   *
   * @param offsetDays the offset in day relative to today
   * @return a <code>Calendar</code> instance that is the begin of the day with the specified offset
   */
  public static GregorianCalendar getRelativeCalendar(int offsetDays) {
    GregorianCalendar today = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    return getRelativeCalendar(today, offsetDays);
  }

  /**
   * Computes the day that has the given offset in days from the specified
   * <em>from</em> date and returns it as an instance of <code>Calendar</code>.
   *
   * @param from the base date as <code>Calendar</code> instance
   * @param offsetDays the offset in day relative to today
   * @return a <code>Calendar</code> instance that is the begin of the day with the specified offset from the given day
   */
  public static GregorianCalendar getRelativeCalendar(Calendar from, int offsetDays) {
    GregorianCalendar temp
            = new GregorianCalendar(
                    from.get(Calendar.YEAR),
                    from.get(Calendar.MONTH),
                    from.get(Calendar.DATE),
                    0,
                    0,
                    0);
    temp.add(Calendar.DATE, offsetDays);
    return temp;
  }
}
