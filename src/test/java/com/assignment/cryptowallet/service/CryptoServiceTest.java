package com.assignment.cryptowallet.service;

import com.assignment.cryptowallet.dto.AssetDto;
import com.assignment.cryptowallet.dto.ResponseDto;
import com.assignment.cryptowallet.dto.WalletDto;
import com.assignment.cryptowallet.mapper.WalletMapper;
import com.assignment.cryptowallet.model.Asset;
import com.assignment.cryptowallet.model.Currency;
import com.assignment.cryptowallet.model.Wallet;
import com.assignment.cryptowallet.repository.AssetRepository;
import com.assignment.cryptowallet.repository.CurrencyRepository;
import com.assignment.cryptowallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CryptoServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    WalletMapper walletMapper;

    @InjectMocks
    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(cryptoService, "apiUrl", "https://api.coincap.io/v2");
    }

    @Test
    void fetchPrice_ShouldReturnPrice() {
        // Mock API response
        String symbol = "BTC";
        String url = "https://api.coincap.io/v2/assets/" + symbol.toLowerCase();
        Map<String, Object> mockData = Map.of("priceUsd", "65000.00");
        Map<String, Object> mockResponse = Map.of("data", mockData);

        when(restTemplate.getForEntity(url, Map.class)).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Call the method under test
        CompletableFuture<BigDecimal> priceFuture = cryptoService.fetchPrice(symbol);

        // Assertions
        assertNotNull(priceFuture);
        assertEquals(BigDecimal.valueOf(65000.00).setScale(2), priceFuture.join());
        verify(restTemplate, times(1)).getForEntity(url, Map.class);
    }

    @Test
    void registerWallet_ShouldSaveWalletAndAssets() {
        // Mock WalletDto and AssetDto inputs
        AssetDto bitcoin = new AssetDto("BTC", BigDecimal.valueOf(1.0), BigDecimal.valueOf(30000.0));
        AssetDto ethereum = new AssetDto("ETH", BigDecimal.valueOf(2.0), BigDecimal.valueOf(2000.0));
        WalletDto walletDto = new WalletDto(List.of(bitcoin, ethereum));

        // Mock Wallet and Assets
        Wallet wallet = new Wallet();
        Asset asset1 = new Asset("BTC", BigDecimal.valueOf(1.0), BigDecimal.valueOf(30000.00));
        Asset asset2 = new Asset("ETH", BigDecimal.valueOf(2.0), BigDecimal.valueOf(2000.00));
        wallet.setAssets(List.of(asset1, asset2));

        Currency currency1 = new Currency("bitcoin", "BTC", BigDecimal.valueOf(30000.00));
        Currency currency2 = new Currency("ethereum", "ETH", BigDecimal.valueOf(2000.00));
        // Mock WalletMapper behavior
        when(walletMapper.toEntity(walletDto)).thenReturn(wallet);

        // Mock Repository Behavior for Wallet
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet savedWallet = invocation.getArgument(0);
            savedWallet.setId(1L); // Simulate database-generated ID
            return savedWallet;
        });
        when(currencyRepository.findBySymbol("BTC")).thenReturn(Optional.of(currency1));
        when(currencyRepository.findBySymbol("ETH")).thenReturn(Optional.of(currency2));
        when(currencyRepository.findBySymbolIn(anyList())).thenReturn(Optional.of(List.of(currency1, currency2)));

        // Mock Repository Behavior for Assets
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> {
            Asset savedAsset = invocation.getArgument(0);
            savedAsset.setId((long) (Math.random() * 100)); // Simulate database-generated ID
            return savedAsset;
        });

        // Call the method under test
        ResponseDto responseDto = cryptoService.registerWallet(walletDto);

        // Assertions
        assertNotNull(responseDto);
        assertEquals(BigDecimal.valueOf(34000.00).setScale(2), responseDto.totalValue()); // Verify the total value

        // Verifications
        verify(walletMapper, times(1)).toEntity(walletDto); // Ensure mapper is invoked
        verify(walletRepository, times(1)).save(any(Wallet.class)); // Ensure wallet is saved once
    }

    @Test
    void registerWallet_ShouldRegisterAndCalculateResponse() {
        // Mock DTOs
        AssetDto bitcoin = new AssetDto("BTC", BigDecimal.valueOf(1), BigDecimal.valueOf(30000.00));
        AssetDto ethereum = new AssetDto("ETH", BigDecimal.valueOf(2), BigDecimal.valueOf(2000.00));
        WalletDto walletDto = new WalletDto(List.of(bitcoin, ethereum));

        // Mock Entities
        Wallet wallet = new Wallet();
        Asset asset1 = new Asset("BTC", BigDecimal.ONE, BigDecimal.valueOf(30000.00));
        Asset asset2 = new Asset("ETH", BigDecimal.valueOf(2), BigDecimal.valueOf(2000.00));
        wallet.setAssets(List.of(asset1, asset2));

        when(walletMapper.toEntity(walletDto)).thenReturn(wallet);
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        // Mock Response Statistics
        Currency btcCurrency = new Currency("Bitcoin", "BTC", BigDecimal.valueOf(35000.00));
        Currency ethCurrency = new Currency("Ethereum", "ETH", BigDecimal.valueOf(2200.00));
        List<Currency> currencies = List.of(btcCurrency, ethCurrency);
        List<String> symbols = List.of("BTC,ETH");
        when(currencyRepository.findBySymbol("BTC")).thenReturn(Optional.of(btcCurrency));
        when(currencyRepository.findBySymbol("ETH")).thenReturn(Optional.of(ethCurrency));
        when(currencyRepository.findBySymbolIn(anyList())).thenReturn(Optional.of(currencies));
        when(currencyRepository.getCurrenciesBySymbol("BTC")).thenReturn(Optional.of(btcCurrency));
        when(currencyRepository.getCurrenciesBySymbol("ETH")).thenReturn(Optional.of(ethCurrency));

        ResponseDto responseDto = cryptoService.registerWallet(walletDto);

        assertNotNull(responseDto);
        assertEquals(BigDecimal.valueOf(39400.00).setScale(2), responseDto.totalValue());
        assertEquals("BTC", responseDto.bestAsset());
        assertEquals("ETH", responseDto.worstAsset());

        verify(walletRepository, times(1)).save(wallet);
        verify(currencyRepository, times(6)).findBySymbol(anyString());
    }

    @Test
    void registerWallet_ShouldThrowException_WhenWalletDtoIsNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> cryptoService.registerWallet(null));
        assertEquals("wallet is null", exception.getMessage());
    }

    @Test
    void registerWallet_ShouldCalculatePerformanceCorrectly() {
        // Mock WalletDto and Assets
        AssetDto bitcoin = new AssetDto("BTC", BigDecimal.valueOf(1), BigDecimal.valueOf(30000.00));
        AssetDto ethereum = new AssetDto("ETH", BigDecimal.valueOf(2), BigDecimal.valueOf(2000.00));
        WalletDto walletDto = new WalletDto(List.of(bitcoin, ethereum));

        // Mock Entities
        Wallet wallet = new Wallet();
        Asset asset1 = new Asset("BTC", BigDecimal.ONE, BigDecimal.valueOf(30000.00));
        Asset asset2 = new Asset("ETH", BigDecimal.valueOf(2), BigDecimal.valueOf(2000.00));
        wallet.setAssets(List.of(asset1, asset2));

        // Mock Mapper and Repository
        when(walletMapper.toEntity(walletDto)).thenReturn(wallet);
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        // Mock Currencies
        Currency btcCurrency = new Currency("Bitcoin", "BTC", BigDecimal.valueOf(35000.00));
        Currency ethCurrency = new Currency("Ethereum", "ETH", BigDecimal.valueOf(2200.00));
        when(currencyRepository.findBySymbol("BTC")).thenReturn(Optional.of(btcCurrency));
        when(currencyRepository.findBySymbol("ETH")).thenReturn(Optional.of(ethCurrency));
        when(currencyRepository.findBySymbolIn(anyList())).thenReturn(Optional.of(List.of(btcCurrency, ethCurrency)));
        when(currencyRepository.getCurrenciesBySymbol("BTC")).thenReturn(Optional.of(btcCurrency));
        when(currencyRepository.getCurrenciesBySymbol("ETH")).thenReturn(Optional.of(ethCurrency));
        // Call registerWallet (indirectly testing calculatePerformance)
        ResponseDto responseDto = cryptoService.registerWallet(walletDto);

        // Assertions for Response Statistics
        assertNotNull(responseDto);
        assertEquals(BigDecimal.valueOf(39400.00).setScale(2), responseDto.totalValue());
        assertEquals("BTC", responseDto.bestAsset());
        assertEquals("ETH", responseDto.worstAsset());
        assertEquals(BigDecimal.valueOf(17.00).setScale(2), responseDto.bestPerformance());
        assertEquals(BigDecimal.valueOf(10.00).setScale(2), responseDto.worstPerformance());

        // Verify interactions
        verify(walletRepository, times(1)).save(wallet);
        verify(currencyRepository, times(6)).findBySymbol(anyString());
    }

    @Test
    void registerWallet_ShouldRegisterCurrenciesCorrectly() {
        // Mock WalletDto and Assets
        AssetDto bitcoin = new AssetDto("BTC", BigDecimal.valueOf(1), BigDecimal.valueOf(30000.00));
        AssetDto ethereum = new AssetDto("ETH", BigDecimal.valueOf(2), BigDecimal.valueOf(2000.00));
        WalletDto walletDto = new WalletDto(List.of(bitcoin, ethereum));

        // Mock Mapper
        Wallet wallet = new Wallet();
        Asset asset1 = new Asset("BTC", BigDecimal.ONE, BigDecimal.valueOf(30000.00));
        Asset asset2 = new Asset("ETH", BigDecimal.valueOf(2), BigDecimal.valueOf(2000.00));
        wallet.setAssets(List.of(asset1, asset2));
        when(walletMapper.toEntity(walletDto)).thenReturn(wallet);

        // Mock getCurrencyName response (used in getCurrencyPrice)
        String searchUrlBTC = "https://api.coincap.io/v2/assets?search=BTC&limit=1";
        String searchUrlETH = "https://api.coincap.io/v2/assets?search=ETH&limit=1";

        String mockResponseBTC = """
                {
                    "data": [
                        { "name": "Bitcoin" }
                    ]
                }
                """;

        String mockResponseETH = """
                {
                    "data": [
                        { "name": "Ethereum" }
                    ]
                }
                """;

        when(restTemplate.getForEntity(searchUrlBTC, String.class)).thenReturn(new ResponseEntity<>(mockResponseBTC, HttpStatus.OK));
        when(restTemplate.getForEntity(searchUrlETH, String.class)).thenReturn(new ResponseEntity<>(mockResponseETH, HttpStatus.OK));

        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(new ResponseEntity<>("{\"data\":[{\"priceUsd\":\"35000.00\"}]}", HttpStatus.OK));
        Currency btcCurrency = new Currency("Bitcoin", "BTC", BigDecimal.valueOf(35000.00));
        Currency ethCurrency = new Currency("Ethereum", "ETH", BigDecimal.valueOf(2200.00));
        when(currencyRepository.findBySymbol("BTC")).thenReturn(Optional.of(btcCurrency));
        when(currencyRepository.findBySymbol("ETH")).thenReturn(Optional.of(ethCurrency));
        when(currencyRepository.findBySymbolIn(anyList())).thenReturn(Optional.of(List.of(btcCurrency, ethCurrency)));

        // Call registerWallet (indirectly testing getCurrencyName and getCurrencyPrice)
        ResponseDto responseDto = cryptoService.registerWallet(walletDto);

        // Assertions
        assertNotNull(responseDto);
        assertEquals(BigDecimal.valueOf(39400.00).setScale(2), responseDto.totalValue());
        assertEquals("BTC", responseDto.bestAsset());
        assertEquals("ETH", responseDto.worstAsset());
        assertEquals(BigDecimal.valueOf(17.00).setScale(2), responseDto.bestPerformance());
        assertEquals(BigDecimal.valueOf(10.00).setScale(2), responseDto.worstPerformance());

        // Verify interactions
        verify(currencyRepository, times(1)).saveAll(anyList()); // One for each asset
        verify(walletRepository, times(1)).save(wallet);
    }
}
