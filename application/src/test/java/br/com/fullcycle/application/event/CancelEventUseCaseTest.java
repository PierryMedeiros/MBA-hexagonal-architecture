package br.com.fullcycle.application.event;

import br.com.fullcycle.application.repository.InMemoryEventRepository;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventUseCaseTest {

    @Test
    @DisplayName("Deve cancelar um evento ativo")
    public void testCancelEvent() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var expectedStatus = "CANCELLED";
        final var expectedId = anEvent.eventId().value();

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        final var input = new CancelEventUseCase.Input(expectedId);

        // when
        final var useCase = new CancelEventUseCase(eventRepository);
        final var output = useCase.execute(input);

        // then
        Assertions.assertEquals(expectedId, output.id());
        Assertions.assertEquals(expectedStatus, output.status());

        final var actualEvent = eventRepository.eventOfId(anEvent.eventId()).get();
        Assertions.assertTrue(actualEvent.cancelled());
    }

    @Test
    @DisplayName("Não deve cancelar um evento que não existe")
    public void testCancelEventThatDoesNotExist() {
        // given
        final var expectedError = "Event not found";
        final var expectedId = EventId.unique().value();

        final var eventRepository = new InMemoryEventRepository();

        final var input = new CancelEventUseCase.Input(expectedId);

        // when
        final var useCase = new CancelEventUseCase(eventRepository);
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(input));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Não deve cancelar um evento já cancelado")
    public void testCancelEventAlreadyCancelled() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        anEvent.cancel();

        final var expectedError = "Event already cancelled";

        final var eventRepository = new InMemoryEventRepository();
        eventRepository.create(anEvent);

        final var input = new CancelEventUseCase.Input(anEvent.eventId().value());

        // when
        final var useCase = new CancelEventUseCase(eventRepository);
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(input));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }
}
