package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.repository.InMemoryTicketRepository;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventTicketsUseCaseTest {

    @Test
    @DisplayName("Deve cancelar todos os ingressos de um evento")
    public void testCancelEventTickets() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var aCustomer = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");
        final var anotherCustomer = Customer.newCustomer("Jane Doe", "111.456.789-01", "jane.doe@gmail.com");

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(anotherCustomer.customerId(), anEvent.eventId()));

        final var expectedCancelledTickets = 2;

        final var input = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(input);

        // then
        Assertions.assertEquals(anEvent.eventId().value(), output.eventId());
        Assertions.assertEquals(expectedCancelledTickets, output.cancelledTickets());

        ticketRepository.ticketsByEventId(anEvent.eventId())
                .forEach(ticket -> Assertions.assertEquals(TicketStatus.CANCELLED, ticket.status()));
    }

    @Test
    @DisplayName("Não deve cancelar os ingressos de outro evento")
    public void testCancelOnlyTicketsOfTheGivenEvent() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var anotherEvent = Event.newEvent("Rock in Rio", "2021-02-02", 10, aPartner);

        final var aCustomer = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));
        final var otherTicket = ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anotherEvent.eventId()));

        final var input = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(input);

        // then
        final var untouched = ticketRepository.ticketOfId(otherTicket.ticketId()).get();
        Assertions.assertEquals(TicketStatus.PENDING, untouched.status());
    }

    @Test
    @DisplayName("A cascata de cancelamento deve ser idempotente")
    public void testCancelEventTicketsIsIdempotent() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var aCustomer = Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com");

        final var ticketRepository = new InMemoryTicketRepository();
        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));

        final var input = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(input);

        // when reprocessing
        Assertions.assertDoesNotThrow(() -> useCase.execute(input));

        // then
        ticketRepository.ticketsByEventId(anEvent.eventId())
                .forEach(ticket -> Assertions.assertEquals(TicketStatus.CANCELLED, ticket.status()));
    }
}
