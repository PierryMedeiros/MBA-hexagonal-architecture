package br.com.fullcycle.application.usecases;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CancelEventUseCaseIT extends IntegrationTest {

    @Autowired
    private CancelEventUseCase useCase;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cancelar um evento ativo e refletir na persistência")
    public void testCancel() {
        // given
        final var partner = createPartner("41.536.538/0001-00", "john.doe@gmail.com", "John Doe");
        final var anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, partner));

        final var expectedStatus = "CANCELLED";

        final var input = new CancelEventUseCase.Input(anEvent.eventId().value());

        // when
        final var output = useCase.execute(input);

        // then
        Assertions.assertEquals(anEvent.eventId().value(), output.id());
        Assertions.assertEquals(expectedStatus, output.status());

        final var persisted = eventRepository.eventOfId(anEvent.eventId()).get();
        Assertions.assertTrue(persisted.cancelled());
    }

    @Test
    @DisplayName("Não deve cancelar um evento que não existe")
    public void testCancelEvent_whenEventDoesntExists_ShouldThrowError() {
        // given
        final var expectedError = "Event not found";
        final var input = new CancelEventUseCase.Input(EventId.unique().value());

        // when
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(input));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Não deve cancelar um evento já cancelado")
    public void testCancelEvent_whenAlreadyCancelled_ShouldThrowError() {
        // given
        final var partner = createPartner("41.536.538/0001-00", "john.doe@gmail.com", "John Doe");
        final var anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, partner));

        useCase.execute(new CancelEventUseCase.Input(anEvent.eventId().value()));

        final var expectedError = "Event already cancelled";
        final var input = new CancelEventUseCase.Input(anEvent.eventId().value());

        // when
        final var actualException = Assertions.assertThrows(ValidationException.class, () -> useCase.execute(input));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    private Partner createPartner(final String cnpj, final String email, final String name) {
        return partnerRepository.create(Partner.newPartner(name, cnpj, email));
    }
}
