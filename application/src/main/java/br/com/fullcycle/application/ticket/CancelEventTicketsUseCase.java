package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.UseCase;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;

import java.util.List;
import java.util.Objects;

public class CancelEventTicketsUseCase
        extends UseCase<CancelEventTicketsUseCase.Input, CancelEventTicketsUseCase.Output> {

    private final TicketRepository ticketRepository;

    public CancelEventTicketsUseCase(final TicketRepository ticketRepository) {
        this.ticketRepository = Objects.requireNonNull(ticketRepository);
    }

    @Override
    public Output execute(final Input input) {
        final List<Ticket> tickets = ticketRepository.ticketsByEventId(EventId.with(input.eventId()));

        tickets.forEach(ticket -> {
            ticket.cancel();
            ticketRepository.update(ticket);
        });

        return new Output(input.eventId(), tickets.size());
    }

    public record Input(String eventId) {
    }

    public record Output(String eventId, int cancelledTickets) {
    }
}
