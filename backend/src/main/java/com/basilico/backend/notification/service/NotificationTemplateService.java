package com.basilico.backend.notification.service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.basilico.backend.booking.entity.Booking;
import com.basilico.backend.order.entity.CustomerOrder;
import com.basilico.backend.order.entity.FulfilmentType;
import com.basilico.backend.order.entity.OrderItem;
import com.basilico.backend.order.entity.PaymentStatus;
import com.basilico.backend.order.entity.TimingType;

@Service
public class NotificationTemplateService {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK);
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.UK);
	private static final String BRAND = "Basilico - Simple Italian";
	private static final String RESTAURANT_ADDRESS = """
			Basilico - Simple Italian
			41 Trinity Street
			Dorchester
			Dorset
			DT1 1TT
			""";
	private static final String PHONE = "07424 642900";

	public EmailContent orderReceived(CustomerOrder order) {
		String subject = "Basilico order received - " + order.getOrderReference();
		String text = """
				Hi %s,

				We've received your Basilico order.

				Order reference: %s
				Fulfilment: %s
				Requested time: %s

				%s

				Food subtotal: %s
				Delivery: %s
				Total: %s
				%s

				%s

				Customer notes:
				%s

				Please tell us about any allergies or dietary requirements. Customer notes are reviewed by the restaurant but do not guarantee allergen-safe preparation.

				%s
				Dorchester
				""".formatted(
				order.getCustomerFirstName(),
				order.getOrderReference(),
				order.getFulfilmentType(),
				formatOrderTiming(order),
				formatOrderItems(order),
				formatPence(order.getSubtotalPence()),
				order.getDeliveryFeePence() == 0 ? "Free" : formatPence(order.getDeliveryFeePence()),
				formatPence(order.getTotalPence()),
				formatDeliveryEstimate(order),
				order.getFulfilmentType() == FulfilmentType.COLLECTION
						? "Collection from:\n" + RESTAURANT_ADDRESS
						: "Delivery address:\n" + formatDeliveryAddress(order),
				order.getOrderNotes() == null ? "No notes supplied." : order.getOrderNotes(),
				BRAND
		);
		return content(subject, text);
	}

	public EmailContent orderAccepted(CustomerOrder order) {
		return content("Your Basilico order has been accepted", """
				Hi %s,

				Your Basilico order has been accepted.

				Order reference: %s

				We'll update you again when it is ready.

				%s
				Dorchester
				""".formatted(order.getCustomerFirstName(), order.getOrderReference(), BRAND));
	}

	public EmailContent orderReady(CustomerOrder order) {
		String subject = order.getFulfilmentType() == FulfilmentType.COLLECTION
				? "Your Basilico order is ready for collection"
				: "Your Basilico order is ready";
		String readyCopy = order.getFulfilmentType() == FulfilmentType.COLLECTION
				? "Your Basilico order is ready for collection.\n\nCollection from:\n" + RESTAURANT_ADDRESS
				: "Your Basilico order is ready. We'll handle the next fulfilment step from here.";
		return content(subject, """
				Hi %s,

				%s

				Order reference: %s

				%s
				Dorchester
				""".formatted(order.getCustomerFirstName(), readyCopy, order.getOrderReference(), BRAND));
	}

	public EmailContent orderCancelled(CustomerOrder order) {
		String paymentLine = order.getPaymentStatus() == PaymentStatus.PAID
				? "Please contact Basilico regarding payment/refund arrangements."
				: "No online payment has been marked as completed for this order.";
		return content("Your Basilico order has been cancelled", """
				Hi %s,

				Your Basilico order has been cancelled.

				Order reference: %s

				%s

				If you need help, please call %s.

				%s
				Dorchester
				""".formatted(order.getCustomerFirstName(), order.getOrderReference(), paymentLine, PHONE, BRAND));
	}

	public EmailContent bookingRequestReceived(Booking booking) {
		return content("We've received your booking request", """
				Hi %s,

				We've received your booking request.

				Booking reference: %s
				Date: %s
				Time: %s
				Party size: %d

				Your table is not confirmed yet. We'll confirm your table separately.

				%s
				Dorchester
				""".formatted(
				booking.getFirstName(),
				booking.getBookingReference(),
				formatDate(booking),
				formatTime(booking),
				booking.getPartySize(),
				BRAND
		));
	}

	public EmailContent bookingConfirmed(Booking booking) {
		return content("Your table at Basilico is confirmed", """
				Hi %s,

				Your table at Basilico is confirmed.

				Booking reference: %s
				Date: %s
				Time: %s
				Party size: %d

				%s
				Phone: %s

				%s
				Dorchester
				""".formatted(
				booking.getFirstName(),
				booking.getBookingReference(),
				formatDate(booking),
				formatTime(booking),
				booking.getPartySize(),
				RESTAURANT_ADDRESS,
				PHONE,
				BRAND
		));
	}

	public EmailContent bookingDeclined(Booking booking) {
		return content("Your Basilico booking request", """
				Hi %s,

				We're sorry, but we are unable to confirm your booking request.

				Booking reference: %s
				Date: %s
				Time: %s
				Party size: %d

				If you need help, please call %s.

				%s
				Dorchester
				""".formatted(
				booking.getFirstName(),
				booking.getBookingReference(),
				formatDate(booking),
				formatTime(booking),
				booking.getPartySize(),
				PHONE,
				BRAND
		));
	}

	public EmailContent bookingCancelled(Booking booking) {
		return content("Your Basilico booking has been cancelled", """
				Hi %s,

				Your Basilico booking has been cancelled.

				Booking reference: %s
				Date: %s
				Time: %s
				Party size: %d

				If you need help, please call %s.

				%s
				Dorchester
				""".formatted(
				booking.getFirstName(),
				booking.getBookingReference(),
				formatDate(booking),
				formatTime(booking),
				booking.getPartySize(),
				PHONE,
				BRAND
		));
	}

	public EmailContent manualMessage(String subject, String message) {
		return content(subject.trim(), message.trim());
	}

	private EmailContent content(String subject, String text) {
		return new EmailContent(subject, text, toHtml(text));
	}

	private String formatOrderItems(CustomerOrder order) {
		StringBuilder builder = new StringBuilder("Order items:\n");
		for (OrderItem item : order.getItems()) {
			builder.append("- ")
					.append(item.getQuantity())
					.append(" x ")
					.append(item.getProductNameSnapshot())
					.append(" - ")
					.append(formatPence(item.getLineTotalPence()))
					.append('\n');
			item.getToppings().forEach(topping -> builder.append("  + ")
					.append(topping.getToppingNameSnapshot())
					.append(" - ")
					.append(formatPence(topping.getPricePenceSnapshot()))
					.append('\n'));
		}
		return builder.toString().trim();
	}

	private String formatOrderTiming(CustomerOrder order) {
		if (order.getTimingType() == TimingType.ASAP) {
			return "ASAP";
		}

		return order.getRequestedDate().format(DATE_FORMAT) + " at " + order.getRequestedTime().format(TIME_FORMAT);
	}

	private String formatDeliveryAddress(CustomerOrder order) {
		StringBuilder builder = new StringBuilder();
		builder.append(order.getDeliveryAddressLine1()).append('\n');
		if (order.getDeliveryAddressLine2() != null) {
			builder.append(order.getDeliveryAddressLine2()).append('\n');
		}
		builder.append(order.getDeliveryCity()).append('\n');
		builder.append(order.getDeliveryPostcode());
		return builder.toString();
	}

	private String formatDeliveryEstimate(CustomerOrder order) {
		if (order.getFulfilmentType() != FulfilmentType.DELIVERY
				|| order.getEstimatedDeliveryMinutes() == null) {
			return "";
		}

		return "Estimated delivery: approximately " + order.getEstimatedDeliveryMinutes() + " minutes.";
	}

	private String formatDate(Booking booking) {
		return booking.getBookingDate().format(DATE_FORMAT);
	}

	private String formatTime(Booking booking) {
		return booking.getBookingTime().format(TIME_FORMAT);
	}

	private String formatPence(int pence) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.UK);
		formatter.setMinimumFractionDigits(2);
		formatter.setMaximumFractionDigits(2);
		return formatter.format(pence / 100.0);
	}

	private String toHtml(String text) {
		String escaped = text
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
		return "<div style=\"font-family:Arial,sans-serif;line-height:1.5;color:#24211b;\">"
				+ escaped.replace("\n", "<br>")
				+ "</div>";
	}
}
