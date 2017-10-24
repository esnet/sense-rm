package net.es.nsi.cs.lib;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class Client {
/**
  public void reserve(Configuration config) {
       // Build a test reservation.
        Reserve reserve = new Reserve();

        // Set request header information.
        reserve.setCorrelationId(Helper.getUUID());
        reserve.setReplyTo(config.getRequesterEndpoint());
        reserve.setProviderNSA(config.getProviderNsa());
        reserve.setRequesterNSA(config.getRequesterNSA());
        reserve.setGlobalUserName(config.getGlobalUserName());
        reserve.setUserRole(config.getGlobalRole());

        // Set the reservation information.
        reserve.setGlobalReservationId(Helper.getGlobalReservationId());
        reserve.setConnectionId(Helper.getUUID());
        reserve.setDescription(config.getReservationDescription());
        reserve.setSourceStpId(config.getReservationSourceStp());
        reserve.setDestStpId(config.getReservationDestinationStp());
        reserve.setServiceBandwidth(config.getReservationBandwidth());

        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.FULL);

        // Set reservation start time is now.
        GregorianCalendar startTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        startTime.add(Calendar.MINUTE, 2);
        reserve.setStartTime(startTime);

        log.info("Reservation startTime " + df.format(startTime.getTime()) + " " + tf.format(startTime.getTime()));

        // Reservation end time is 5 minutes from now.
        GregorianCalendar endTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        endTime.add(Calendar.MINUTE, 4);
        reserve.setEndTime(endTime);

        log.info("Reservation endTime " + df.format(endTime.getTime()) + " " + tf.format(endTime.getTime()));

        // Get a populated NSI request header.
        CommonHeaderType reserveHeader = reserve.getRequestHeaderType();

        // Get a populated reserve request body.
        ReservationInfoType reserveBody = reserve.getReservationInfoType();

        log.info("Sending reserve header ---\n" + Helper.dump(CommonHeaderType.class, reserveHeader));
        log.info("Sending reserve body ---\n" + Helper.dump(ReservationInfoType.class, reserveBody));

        ConnectionServiceProvider provider = new ConnectionServiceProvider();
        ConnectionProviderPort proxy = provider.getConnectionServiceProviderPort();

        BindingProvider bp = (BindingProvider) proxy;

        Map<String, Object> context = bp.getRequestContext();

        context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, config.getProviderEndpoint());
        context.put(BindingProvider.USERNAME_PROPERTY, config.getProviderUserId());
        context.put(BindingProvider.PASSWORD_PROPERTY, config.getProviderPassword());
        context.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
        context.put(JAXWSProperties.HOSTNAME_VERIFIER, new TestHostnameVerifier());
        TestSecurityProvider.registerProvider();

        Holder<CommonHeaderType> header = new Holder<CommonHeaderType>();
        header.value = reserveHeader;

        try {
            log.info("Sending " + reserveHeader.getCorrelationId());
            proxy.reserve(reserveBody, header);
        }
        catch (ServiceException ex) {
            log.error("Reservation exception - " + ex.getFaultInfo().getErrorId() + " " + ex.getFaultInfo().getText());
            log.error(Helper.dump(ServiceExceptionType.class, ex.getFaultInfo()));
        }

        log.info("Ack recieved " + header.value.getCorrelationId());

        this.reserveConfirm(config, reserveHeader, reserveBody);
    }

    public void reserveConfirm(Configuration config, CommonHeaderType reserveHeader, ReservationInfoType reserveBody) {
        // Get a populated reserve request body.
        ConnectionServiceRequester requester = new ConnectionServiceRequester();
        ConnectionRequesterPort proxy = requester.getConnectionServiceRequesterPort();

        BindingProvider bp = (BindingProvider) proxy;

        Map<String, Object> context = bp.getRequestContext();

        context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://localhost:8084/NSI-2.0/ConnectionServiceRequester");
        context.put(BindingProvider.USERNAME_PROPERTY, config.getProviderUserId());
        context.put(BindingProvider.PASSWORD_PROPERTY, config.getProviderPassword());
        context.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
        context.put(JAXWSProperties.HOSTNAME_VERIFIER, new TestHostnameVerifier());
        TestSecurityProvider.registerProvider();

        reserveHeader.setCorrelationId(Helper.getUUID());
        Holder<CommonHeaderType> header = new Holder<CommonHeaderType>();
        header.value = reserveHeader;

        try {
            log.info("Sending " + reserveHeader.getCorrelationId());
            proxy.reserveConfirmed(reserveBody, header);
        }
        catch (org.ogf.schemas.nsi._2012._03.connection.requester.ServiceException ex) {
            log.error("reserveConfirmed exception - " + ex.getFaultInfo().getErrorId() + " " + ex.getFaultInfo().getText());
            log.error(Helper.dump(ServiceExceptionType.class, ex.getFaultInfo()));
        }

        log.info("Ack recieved " + header.value.getCorrelationId());
    }
    * **/
}

