package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.exception.RequestNotValidException;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.graalvm.compiler.nodes.calc.IntegerDivRemNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class RsServiceTest {
  RsService rsService;

  @Mock RsEventRepository rsEventRepository;
  @Mock UserRepository userRepository;
  @Mock VoteRepository voteRepository;
  @Mock TradeRepository tradeRepository;
  LocalDateTime localDateTime;
  Vote vote;

  @BeforeEach
  void setUp() {
    initMocks(this);
    rsService = new RsService(tradeRepository, rsEventRepository, userRepository, voteRepository);
    localDateTime = LocalDateTime.now();
    vote = Vote.builder().voteNum(2).rsEventId(1).time(localDateTime).userId(1).build();
  }

  @Test
  void shouldVoteSuccess() {
    // given

    UserDto userDto =
        UserDto.builder()
            .voteNum(5)
            .phone("18888888888")
            .gender("female")
            .email("a@b.com")
            .age(19)
            .userName("xiaoli")
            .id(2)
            .build();
    RsEventDto rsEventDto =
        RsEventDto.builder()
            .eventName("event name")
            .id(1)
            .keyword("keyword")
            .voteNum(2)
            .user(userDto)
            .build();

    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
    when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
    // when
    rsService.vote(vote, 1);
    // then
    verify(voteRepository)
        .save(
            VoteDto.builder()
                .num(2)
                .localDateTime(localDateTime)
                .user(userDto)
                .rsEvent(rsEventDto)
                .build());
    verify(userRepository).save(userDto);
    verify(rsEventRepository).save(rsEventDto);
  }

  @Test
  void shouldThrowExceptionWhenUserNotExist() {
    // given
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.empty());
    when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
    //when&then
    assertThrows(
        RuntimeException.class,
        () -> {
          rsService.vote(vote, 1);
        });
  }

  @Test
  void should_throw_exception_when_vote_numbers_is_less_than_user_own() {
    UserDto userDto =
            UserDto.builder()
                    .voteNum(0)
                    .phone("18888888888")
                    .gender("female")
                    .email("a@b.com")
                    .age(19)
                    .userName("xiaoli")
                    .id(2)
                    .build();
    when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
    RsEventDto rsEventDto =
            RsEventDto.builder()
                    .eventName("event name")
                    .id(1)
                    .keyword("keyword")
                    .voteNum(2)
                    .user(userDto)
                    .build();
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));

    assertThrows(
            RuntimeException.class,
            () -> {
              rsService.vote(vote, 1);
            });
  }

  @Test
  void should_buy_success() {
    UserDto userDto =
            UserDto.builder()
                    .voteNum(0)
                    .phone("18888888888")
                    .gender("female")
                    .email("a@b.com")
                    .age(19)
                    .userName("xiaoli")
                    .id(2)
                    .build();
    when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
    RsEventDto rsEventDto =
            RsEventDto.builder()
                    .eventName("event name")
                    .id(1)
                    .keyword("keyword")
                    .voteNum(2)
                    .user(userDto)
                    .build();
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));

    Trade trade = Trade.builder()
            .rank(1)
            .amount(1)
            .build();
    rsService.buy(trade, rsEventDto.getId());

    verify(tradeRepository).save(TradeDto.builder()
            .amount(trade.getAmount())
            .rank(trade.getRank())
            .rsEvent(rsEventDto)
            .build());

    rsEventDto.setRank(trade.getRank());
    verify(rsEventRepository).save(rsEventDto);
  }

  @Test
  void should_throw_rs_event_not_existed_exception() {
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.empty());
    Trade trade = Trade.builder()
            .rank(1)
            .amount(1)
            .build();

    assertThrows(RequestNotValidException.class, () -> rsService.buy(trade, 1));
  }

  @Test
  void should_throw_payment_not_enough_exception() {
    UserDto userDto =
            UserDto.builder()
                    .voteNum(0)
                    .phone("18888888888")
                    .gender("female")
                    .email("a@b.com")
                    .age(19)
                    .userName("xiaoli")
                    .id(2)
                    .build();
    when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
    RsEventDto rsEventDto =
            RsEventDto.builder()
                    .eventName("event name")
                    .id(1)
                    .keyword("keyword")
                    .voteNum(2)
                    .user(userDto)
                    .build();
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));

    TradeDto tradeDto = TradeDto.builder()
            .rsEvent(rsEventDto)
            .rank(1)
            .amount(10)
            .id(1).build();
    when(tradeRepository.findFirstByRankOrderByAmountDesc(anyInt())).thenReturn(Optional.of(tradeDto));

    Trade trade = Trade.builder()
            .rank(1)
            .amount(tradeDto.getAmount() - 1)
            .build();

    assertThrows(RequestNotValidException.class, () -> rsService.buy(trade, 1));
  }
}
