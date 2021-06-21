package one.digitalinnovation.beerstock.service;

import static org.assertj.core.api.Assertions.not;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.matchers.LessThan;
import org.mockito.junit.jupiter.MockitoExtension;

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder;
import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.entity.Beer;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.exception.BeerNotFoundException;
import one.digitalinnovation.beerstock.exception.BeerStockExceededException;
import one.digitalinnovation.beerstock.mapper.BeerMapper;
import one.digitalinnovation.beerstock.repository.BeerRepository;

@ExtendWith(MockitoExtension.class)
public class BeerServiceTest {


    private static final long INVALID_BEER_ID = 1L;

	@Mock
	private BeerRepository beerRepository;

	private BeerMapper beerMapper = BeerMapper.INSTANCE;

	@InjectMocks
	private BeerService beerService;

	@Test
	void whenBeerInformedThenItShouldBeCreated() throws BeerAlreadyRegisteredException {

		// dados de entrada
		BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
		Beer savedBeer = beerMapper.toModel(expectedBeerDTO);

		when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.empty());
		when(beerRepository.save(savedBeer)).thenReturn(savedBeer);

		BeerDTO createdBeer = beerService.createBeer(expectedBeerDTO);

		assertThat(createdBeer.getId(), is(equalTo(expectedBeerDTO.getId())));
		assertThat(createdBeer.getName(), is(equalTo(expectedBeerDTO.getName())));
		assertThat(createdBeer.getBrand(), is(equalTo(expectedBeerDTO.getBrand())));
		assertThat(createdBeer.getQuantity(), is(equalTo(expectedBeerDTO.getQuantity())));

	}

	@Test
	void whenAlreadyRegisteredBeerInformedThenAnExceptionShouldBeThrown() {
		// dados de entrada
		BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
		Beer duplicatedBeer = beerMapper.toModel(expectedBeerDTO);

		// verificação
		when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.of(duplicatedBeer));

		// teste
		assertThrows(BeerAlreadyRegisteredException.class, () -> beerService.createBeer(expectedBeerDTO));
	}

	@Test
	void whenValidBeerNameIsGivenThenReturnABeer() throws BeerNotFoundException {

		// dados de entrada
		BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
		Beer beerNameIsValid = beerMapper.toModel(expectedBeerDTO);

		// verificação
		when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.of(beerNameIsValid));

		BeerDTO beerDTO = beerService.findByName(expectedBeerDTO.getName());

		// teste
		assertThat(beerDTO, is(equalTo(expectedBeerDTO)));
	}

	@Test
	void whenNotRegisteredBeerNameIsGivenThenThrowAnException() throws BeerNotFoundException {

		// dados de entrada
		BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

		// verificação
		when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.empty());

		// teste
		assertThrows(BeerNotFoundException.class, () -> beerService.findByName(expectedBeerDTO.getName()));
	}

	@Test
	void whenListBeerIsCalledThenReturnAListOfBeers() {

		// dados de entrada
		BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
		Beer beer = beerMapper.toModel(beerDTO);

		when(beerRepository.findAll()).thenReturn(Collections.singletonList(beer));

		List<BeerDTO> listBeerDTO = beerService.listAll();

		assertThat(listBeerDTO, is(not(empty())));
		assertThat(listBeerDTO.get(0), is(equalTo(beer)));
	}

	@Test
	void whenListBeerIsCalledThenReturnAnEmptyListOfBeers() {

		when(beerRepository.findAll()).thenReturn(Collections.emptyList());

		List<BeerDTO> listBeerDTO = beerService.listAll();

		assertThat(listBeerDTO, is(empty()));
	}

	@Test
	void whenExclusionIsCalledWithValidIdThenABeerShouldBeDeleted() throws BeerNotFoundException {

		BeerDTO expectedDeletedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
		Beer expectedDeletedBeer = beerMapper.toModel(expectedDeletedBeerDTO);

		// when
		when(beerRepository.findById(expectedDeletedBeerDTO.getId())).thenReturn(Optional.of(expectedDeletedBeer));
		doNothing().when(beerRepository).deleteById(expectedDeletedBeerDTO.getId());

		// then
		beerService.deleteById(expectedDeletedBeerDTO.getId());

		verify(beerRepository, times(1)).findById(expectedDeletedBeerDTO.getId());
		verify(beerRepository, times(1)).deleteById(expectedDeletedBeerDTO.getId());
	}

	@Test
	void whenIncrementIsCalledThenIncrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
		// given
		BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
		Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

		// when
		when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
		when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);

		int quantityToIncrement = 10;
		int expectedQuantityAfterIncrement = expectedBeerDTO.getQuantity() + quantityToIncrement;

		// then
		BeerDTO incrementedBeerDTO = beerService.increment(expectedBeerDTO.getId(), quantityToIncrement);

		assertThat(expectedQuantityAfterIncrement, equalTo(incrementedBeerDTO.getQuantity()));
		assertThat(expectedQuantityAfterIncrement, lessThan(expectedBeerDTO.getMax()));
	}

	@Test
	void whenIncrementIsGreatherThanMaxThenThrowException() {
		BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
		Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

		when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));

		int quantityToIncrement = 80;
		assertThrows(BeerStockExceededException.class,
				() -> beerService.increment(expectedBeerDTO.getId(), quantityToIncrement));
	}

	@Test
	void whenIncrementAfterSumIsGreatherThanMaxThenThrowException() {
		BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
		Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

		when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));

		int quantityToIncrement = 45;
		assertThrows(BeerStockExceededException.class,
				() -> beerService.increment(expectedBeerDTO.getId(), quantityToIncrement));
	}

	@Test
	void whenIncrementIsCalledWithInvalidIdThenThrowException() {
		int quantityToIncrement = 10;

		when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());

		assertThrows(BeerNotFoundException.class, () -> beerService.increment(INVALID_BEER_ID, quantityToIncrement));
	}

}
