package com.softwaremagico.kt.core.controller;

/*-
 * #%L
 * Kendo Tournament Manager (Core)
 * %%
 * Copyright (C) 2021 - 2023 Softwaremagico
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.softwaremagico.kt.core.controller.models.DuelDTO;
import com.softwaremagico.kt.core.controller.models.FightDTO;
import com.softwaremagico.kt.core.controller.models.GroupDTO;
import com.softwaremagico.kt.core.controller.models.ParticipantDTO;
import com.softwaremagico.kt.core.controller.models.ScoreOfCompetitorDTO;
import com.softwaremagico.kt.core.controller.models.ScoreOfTeamDTO;
import com.softwaremagico.kt.core.controller.models.DTO;
import com.softwaremagico.kt.core.controller.models.TournamentDTO;
import com.softwaremagico.kt.core.converters.DuelConverter;
import com.softwaremagico.kt.core.converters.FightConverter;
import com.softwaremagico.kt.core.converters.GroupConverter;
import com.softwaremagico.kt.core.converters.ParticipantConverter;
import com.softwaremagico.kt.core.converters.ScoreOfCompetitorConverter;
import com.softwaremagico.kt.core.converters.ScoreOfTeamConverter;
import com.softwaremagico.kt.core.converters.TeamConverter;
import com.softwaremagico.kt.core.converters.TournamentConverter;
import com.softwaremagico.kt.core.converters.models.GroupConverterRequest;
import com.softwaremagico.kt.core.converters.models.ParticipantConverterRequest;
import com.softwaremagico.kt.core.converters.models.ScoreOfCompetitorConverterRequest;
import com.softwaremagico.kt.core.converters.models.ScoreOfTeamConverterRequest;
import com.softwaremagico.kt.core.converters.models.TeamConverterRequest;
import com.softwaremagico.kt.core.exceptions.GroupNotFoundException;
import com.softwaremagico.kt.core.providers.GroupProvider;
import com.softwaremagico.kt.core.providers.RankingProvider;
import com.softwaremagico.kt.core.score.CompetitorRanking;
import com.softwaremagico.kt.persistence.entities.Group;
import com.softwaremagico.kt.persistence.entities.Team;
import com.softwaremagico.kt.persistence.values.ScoreType;
import com.softwaremagico.kt.persistence.values.TournamentType;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class RankingController {
    private static final int CACHE_EXPIRATION_TIME = 10 * 60 * 1000;

    private final GroupProvider groupProvider;

    private final GroupConverter groupConverter;

    private final TournamentConverter tournamentConverter;

    private final FightConverter fightConverter;

    private final TeamConverter teamConverter;

    private final DuelConverter duelConverter;

    private final ParticipantConverter participantConverter;

    private final RankingProvider rankingProvider;

    private final ScoreOfCompetitorConverter scoreOfCompetitorConverter;

    private final ScoreOfTeamConverter scoreOfTeamConverter;

    public RankingController(GroupProvider groupProvider, GroupConverter groupConverter,
                             TournamentConverter tournamentConverter, FightConverter fightConverter,
                             TeamConverter teamConverter, DuelConverter duelConverter, ParticipantConverter participantConverter,
                             RankingProvider rankingProvider, ScoreOfCompetitorConverter scoreOfCompetitorConverter,
                             ScoreOfTeamConverter scoreOfTeamConverter) {
        this.groupProvider = groupProvider;
        this.groupConverter = groupConverter;
        this.tournamentConverter = tournamentConverter;
        this.fightConverter = fightConverter;
        this.teamConverter = teamConverter;
        this.duelConverter = duelConverter;
        this.participantConverter = participantConverter;
        this.rankingProvider = rankingProvider;
        this.scoreOfCompetitorConverter = scoreOfCompetitorConverter;
        this.scoreOfTeamConverter = scoreOfTeamConverter;
    }

    private static Set<ParticipantDTO> getParticipants(List<DTO> teams) {
        final Set<ParticipantDTO> allCompetitors = new HashSet<>();
        for (final DTO team : teams) {
            allCompetitors.addAll(team.getMembers());
        }
        return allCompetitors;
    }

    private boolean checkLevel(TournamentDTO tournament) {
        return tournament == null || tournament.getType() != TournamentType.KING_OF_THE_MOUNTAIN;
    }

    public List<DTO> getTeamsRanking(GroupDTO groupDTO) {
        return teamConverter.convertAll(rankingProvider.getTeamsRanking(groupConverter.reverse(groupDTO))
                .stream().map(TeamConverterRequest::new).toList());
    }

    public List<ScoreOfTeamDTO> getTeamsScoreRankingFromGroup(Integer groupId) {
        final Group group = groupProvider.getGroup(groupId);
        if (group == null) {
            throw new GroupNotFoundException(this.getClass(), "Group with id" + groupId + " not found!");
        }
        return getTeamsScoreRanking(groupConverter.convert(new GroupConverterRequest(group)));
    }

    public List<ScoreOfTeamDTO> getTeamsScoreRankingFromTournament(Integer tournamentId) {
        return scoreOfTeamConverter.convertAll(rankingProvider.getTeamsScoreRankingFromTournament(tournamentId)
                .stream().map(ScoreOfTeamConverterRequest::new).toList());
    }

    public List<ScoreOfTeamDTO> getTeamsScoreRanking(GroupDTO groupDTO) {
        if (groupDTO == null) {
            return new ArrayList<>();
        }
        return getTeamsScoreRanking(groupDTO.getTournament().getTournamentScore().getScoreType(),
                groupDTO.getTeams(), groupDTO.getFights(), groupDTO.getUnties(), checkLevel(groupDTO.getTournament()));
    }

    public List<ScoreOfTeamDTO> getTeamsScoreRanking(ScoreType type, List<DTO> teams, List<FightDTO> fights, List<DuelDTO> unties,
                                                     boolean checkLevel) {
        return scoreOfTeamConverter.convertAll(rankingProvider.getTeamsScoreRanking(
                type,
                teamConverter.reverseAll(teams),
                fightConverter.reverseAll(fights),
                duelConverter.reverseAll(unties),
                //Checks ranking for same level or globally.
                checkLevel
        ).stream().map(ScoreOfTeamConverterRequest::new).toList());
    }

    public List<ScoreOfTeamDTO> getTeamsScoreRanking(TournamentDTO tournamentDTO) {
        return scoreOfTeamConverter.convertAll(rankingProvider.getTeamsScoreRanking(tournamentConverter.reverse(tournamentDTO))
                .stream().map(ScoreOfTeamConverterRequest::new).toList());
    }

    /**
     * Return a Hashmap that classify the teams by position (1st, 2nd, 3rd,...)
     *
     * @return classification of the teams
     */
    public Map<Integer, List<DTO>> getTeamsByPosition(GroupDTO groupDTO) {
        final Map<Integer, List<Team>> teamsByPosition = rankingProvider.getTeamsByPosition(groupConverter.reverse(groupDTO));
        final Map<Integer, List<DTO>> teamsByPositionDTO = new HashMap<>();
        teamsByPosition.keySet().forEach(key -> teamsByPositionDTO.put(key, teamConverter.convertAll(teamsByPosition.get(key)
                .stream().map(TeamConverterRequest::new).toList())));
        return teamsByPositionDTO;
    }

    public List<DTO> getFirstTeamsWithDrawScore(GroupDTO groupDTO, Integer maxWinners) {
        final Map<Integer, List<DTO>> teamsByPosition = getTeamsByPosition(groupDTO);
        for (int i = 0; i < maxWinners; i++) {
            final List<DTO> teamsInDraw = teamsByPosition.get(i);
            if (teamsInDraw.size() > 1) {
                return teamsInDraw;
            }
        }
        return new ArrayList<>();
    }

    public DTO getTeam(GroupDTO groupDTO, Integer order) {
        final List<DTO> teamsOrder = getTeamsRanking(groupDTO);
        if (order >= 0 && order < teamsOrder.size()) {
            return teamsOrder.get(order);
        }
        return null;
    }

    public List<ScoreOfCompetitorDTO> getCompetitorsScoreRankingFromGroup(Integer groupId) {
        final Group group = groupProvider.getGroup(groupId);
        if (group == null) {
            throw new GroupNotFoundException(this.getClass(), "Group with id" + groupId + " not found!");
        }
        return getCompetitorsScoreRanking(groupConverter.convert(new GroupConverterRequest(group)));
    }

    public List<ScoreOfCompetitorDTO> getCompetitorsScoreRanking(GroupDTO groupDTO) {
        return getCompetitorsScoreRanking(getParticipants(groupDTO.getTeams()), groupDTO.getFights(), groupDTO.getUnties(), groupDTO.getTournament());
    }

    public List<ScoreOfCompetitorDTO> getCompetitorsScoreRankingFromTournament(Integer tournamentId) {
        return scoreOfCompetitorConverter.convertAll(rankingProvider.getCompetitorsScoreRankingFromTournament(tournamentId)
                .stream().map(ScoreOfCompetitorConverterRequest::new).toList());
    }

    public List<ScoreOfCompetitorDTO> getCompetitorsScoreRanking(TournamentDTO tournamentDTO) {
        return scoreOfCompetitorConverter.convertAll(rankingProvider.getCompetitorsScoreRanking(tournamentConverter.reverse(tournamentDTO))
                .stream().map(ScoreOfCompetitorConverterRequest::new).toList());
    }

    public List<ScoreOfCompetitorDTO> getCompetitorsScoreRanking(Set<ParticipantDTO> competitors, List<FightDTO> fights, List<DuelDTO> unties,
                                                                 TournamentDTO tournamentDTO) {
        return scoreOfCompetitorConverter.convertAll(rankingProvider.getCompetitorsScoreRanking(
                participantConverter.reverseAll(competitors),
                fightConverter.reverseAll(fights),
                duelConverter.reverseAll(unties),
                tournamentConverter.reverse(tournamentDTO)
        ).stream().map(ScoreOfCompetitorConverterRequest::new).toList());
    }

    public List<ScoreOfCompetitorDTO> getCompetitorsGlobalScoreRanking(Collection<ParticipantDTO> competitors) {
        return getCompetitorsGlobalScoreRanking(competitors, ScoreType.DEFAULT);
    }

    @Cacheable("competitors-ranking")
    public List<ScoreOfCompetitorDTO> getCompetitorsGlobalScoreRanking(Collection<ParticipantDTO> competitors, ScoreType scoreType) {
        return scoreOfCompetitorConverter.convertAll(rankingProvider.getCompetitorsGlobalScoreRanking(
                        participantConverter.reverseAll(competitors),
                        scoreType
                )
                .stream().map(ScoreOfCompetitorConverterRequest::new).toList());
    }

    @Cacheable("competitors-ranking")
    public List<ScoreOfCompetitorDTO> getCompetitorGlobalRanking(ScoreType scoreType) {
        return scoreOfCompetitorConverter.convertAll(rankingProvider.getCompetitorGlobalRanking(scoreType).stream()
                .map(ScoreOfCompetitorConverterRequest::new).collect(Collectors.toSet()));
    }

    public CompetitorRanking getCompetitorRanking(ParticipantDTO participantDTO) {
        return rankingProvider.getCompetitorRanking(participantConverter.reverse(participantDTO));
    }

    public ScoreOfCompetitorDTO getScoreRanking(GroupDTO groupDTO, ParticipantDTO competitor) {
        return scoreOfCompetitorConverter.convert(new ScoreOfCompetitorConverterRequest(rankingProvider
                .getScoreRanking(groupConverter.reverse(groupDTO), participantConverter.reverse(competitor))));
    }

    public ParticipantDTO getCompetitor(GroupDTO groupDTO, Integer order) {
        return participantConverter.convert(new ParticipantConverterRequest(
                rankingProvider.getCompetitor(groupConverter.reverse(groupDTO), order)));
    }

    public ScoreOfCompetitorDTO getScoreOfCompetitor(GroupDTO groupDTO, Integer order) {
        return scoreOfCompetitorConverter.convert(new ScoreOfCompetitorConverterRequest(rankingProvider
                .getScoreOfCompetitor(groupConverter.reverse(groupDTO), order)));
    }

    public Integer getOrder(GroupDTO groupDTO, DTO teamDTO) {
        return rankingProvider.getOrder(groupConverter.reverse(groupDTO), teamConverter.reverse(teamDTO));
    }

    @CacheEvict(allEntries = true, value = {"ranking", "competitors-ranking"})
    @Scheduled(fixedDelay = CACHE_EXPIRATION_TIME)
    public void reportCacheEvict() {
        //Only for handling Spring cache.
    }
}
