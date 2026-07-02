package br.com.fullcycle.application.usecases;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.ticket.CancelEventTicketsUseCase;
import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.customer.CustomerRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CancelEventTicketsUseCaseIT extends IntegrationTest {

    @Autowired
    private CancelEventTicketsUseCase useCase;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve buscar os ingressos de um evento na persistência real")
    public void testTicketsByEventId() {
        // given
        final var partner = Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, partner);

        final var aCustomer = customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com"));
        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));

        // when
        final var tickets = ticketRepository.ticketsByEventId(anEvent.eventId());

        // then
        Assertions.assertEquals(1, tickets.size());
        Assertions.assertEquals(anEvent.eventId(), tickets.get(0).eventId());
    }

    @Test
    @DisplayName("Deve cancelar todos os ingressos de um evento na persistência real")
    public void testCancelEventTickets() {
        // given
        final var partner = Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, partner);

        final var aCustomer = customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com"));
        final var anotherCustomer = customerRepository.create(Customer.newCustomer("Jane Doe", "111.456.789-01", "jane.doe@gmail.com"));

        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));
        ticketRepository.create(Ticket.newTicket(anotherCustomer.customerId(), anEvent.eventId()));

        final var input = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());

        // when
        final var output = useCase.execute(input);

        // then
        Assertions.assertEquals(2, output.cancelledTickets());

        final var persisted = ticketRepository.ticketsByEventId(anEvent.eventId());
        Assertions.assertEquals(2, persisted.size());
        persisted.forEach(ticket -> Assertions.assertEquals(TicketStatus.CANCELLED, ticket.status()));
    }

    @Test
    @DisplayName("A cascata de cancelamento deve ser idempotente na persistência real")
    public void testCancelEventTicketsIsIdempotent() {
        // given
        final var partner = Partner.newPartner("Disney", "41.536.538/0001-00", "disney@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, partner);

        final var aCustomer = customerRepository.create(Customer.newCustomer("John Doe", "123.456.789-01", "john.doe@gmail.com"));
        ticketRepository.create(Ticket.newTicket(aCustomer.customerId(), anEvent.eventId()));

        final var input = new CancelEventTicketsUseCase.Input(anEvent.eventId().value());
        useCase.execute(input);

        // when reprocessing
        Assertions.assertDoesNotThrow(() -> useCase.execute(input));

        // then
        final var persisted = ticketRepository.ticketsByEventId(anEvent.eventId());
        persisted.forEach(ticket -> Assertions.assertEquals(TicketStatus.CANCELLED, ticket.status()));
    }
}
