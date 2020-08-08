package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.RsEvent;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RsService {
  final TradeRepository tradeRepository;
  final RsEventRepository rsEventRepository;
  final UserRepository userRepository;
  final VoteRepository voteRepository;

  public RsService(TradeRepository tradeRepository, RsEventRepository rsEventRepository, UserRepository userRepository, VoteRepository voteRepository) {
    this.tradeRepository = tradeRepository;
    this.rsEventRepository = rsEventRepository;
    this.userRepository = userRepository;
    this.voteRepository = voteRepository;
  }

  public List<RsEvent> getRsEventList(int page) {
    Order rankAsc = Order.asc("rank");
    Order voteDesc = Order.desc("voteNum");
    Sort sort = Sort.by(Arrays.asList(rankAsc,voteDesc));
    Pageable pageable = PageRequest.of(page - 1, 5, sort);
    return rsEventRepository
            .findAllByOrderByRankAsc(pageable)
            .stream()
            .map(rsEventDto -> RsEvent.builder()
                    .userId(rsEventDto.getUser().getId())
                    .eventName(rsEventDto.getEventName())
                    .keyword(rsEventDto.getKeyword())
                    .rank(rsEventDto.getRank())
                    .voteNum(rsEventDto.getVoteNum())
                    .build()
            ).collect(Collectors.toList());
  }


  public void vote(Vote vote, int rsEventId) {
    Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
    Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
    if (!rsEventDto.isPresent()
        || !userDto.isPresent()
        || vote.getVoteNum() > userDto.get().getVoteNum()) {
      throw new RuntimeException();
    }
    VoteDto voteDto =
        VoteDto.builder()
            .localDateTime(vote.getTime())
            .num(vote.getVoteNum())
            .rsEvent(rsEventDto.get())
            .user(userDto.get())
            .build();
    voteRepository.save(voteDto);
    UserDto user = userDto.get();
    user.setVoteNum(user.getVoteNum() - vote.getVoteNum());
    userRepository.save(user);
    RsEventDto rsEvent = rsEventDto.get();
    rsEvent.setVoteNum(rsEvent.getVoteNum() + vote.getVoteNum());
    rsEventRepository.save(rsEvent);
  }

  @Transactional
  public void buy(Trade trade, int id) {
    Optional<RsEventDto> rsEventDto = rsEventRepository.findById(id);
    if (!rsEventDto.isPresent()) {
      throw new RequestNotValidException("rs event not existed");
    }
    RsEventDto rsEvent = rsEventDto.get();
    TradeDto tradeDto = TradeDto.builder()
            .amount(trade.getAmount())
            .rank(trade.getRank())
            .rsEvent(rsEvent)
            .build();
    Optional<TradeDto> checkRankHistoryPrice = tradeRepository.findFirstByRankOrderByAmountDesc(trade.getRank());
    if (checkRankHistoryPrice.isPresent() && checkRankHistoryPrice.get().getAmount() >= trade.getAmount()) {
      throw new RequestNotValidException("Payment not enough");
    }
    checkRankHistoryPrice.ifPresent(dto -> rsEventRepository.deleteById(dto.getRsEvent().getId()));

    rsEvent.setRank(trade.getRank());
    rsEventRepository.save(rsEvent);
    tradeRepository.save(tradeDto);
  }
}
