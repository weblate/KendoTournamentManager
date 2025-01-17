package com.softwaremagico.kt.core.tournaments;

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

import com.softwaremagico.kt.core.controller.RankingController;
import com.softwaremagico.kt.core.controller.models.ScoreOfTeamDTO;
import com.softwaremagico.kt.core.converters.GroupConverter;
import com.softwaremagico.kt.core.converters.TeamConverter;
import com.softwaremagico.kt.core.converters.models.GroupConverterRequest;
import com.softwaremagico.kt.core.exceptions.InvalidGroupException;
import com.softwaremagico.kt.core.exceptions.LevelNotFinishedException;
import com.softwaremagico.kt.core.managers.CompleteGroupFightManager;
import com.softwaremagico.kt.core.managers.MinimumGroupFightManager;
import com.softwaremagico.kt.core.managers.TeamsOrder;
import com.softwaremagico.kt.core.providers.FightProvider;
import com.softwaremagico.kt.core.providers.GroupLinkProvider;
import com.softwaremagico.kt.core.providers.GroupProvider;
import com.softwaremagico.kt.core.providers.TeamProvider;
import com.softwaremagico.kt.core.providers.TournamentExtraPropertyProvider;
import com.softwaremagico.kt.logger.KendoTournamentLogger;
import com.softwaremagico.kt.persistence.entities.Fight;
import com.softwaremagico.kt.persistence.entities.Group;
import com.softwaremagico.kt.persistence.entities.GroupLink;
import com.softwaremagico.kt.persistence.entities.Tournament;
import com.softwaremagico.kt.persistence.entities.TournamentExtraProperty;
import com.softwaremagico.kt.persistence.values.LeagueFightsOrder;
import com.softwaremagico.kt.persistence.values.TournamentExtraPropertyKey;
import com.softwaremagico.kt.utils.GroupUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TreeTournamentHandler extends LeagueHandler {
    private final GroupProvider groupProvider;
    private final TournamentExtraPropertyProvider tournamentExtraPropertyProvider;
    private final CompleteGroupFightManager completeGroupFightManager;
    private final MinimumGroupFightManager minimumGroupFightManager;
    private final FightProvider fightProvider;
    private final GroupLinkProvider groupLinkProvider;
    private final RankingController rankingController;
    private final GroupConverter groupConverter;
    private final TeamConverter teamConverter;


    public TreeTournamentHandler(GroupProvider groupProvider, TeamProvider teamProvider, GroupConverter groupConverter, RankingController rankingController,
                                 TournamentExtraPropertyProvider tournamentExtraPropertyProvider, CompleteGroupFightManager completeGroupFightManager,
                                 MinimumGroupFightManager minimumGroupFightManager, FightProvider fightProvider, GroupLinkProvider groupLinkProvider,
                                 TeamConverter teamConverter) {
        super(groupProvider, teamProvider, groupConverter, rankingController, tournamentExtraPropertyProvider);
        this.rankingController = rankingController;
        this.groupProvider = groupProvider;
        this.tournamentExtraPropertyProvider = tournamentExtraPropertyProvider;
        this.completeGroupFightManager = completeGroupFightManager;
        this.minimumGroupFightManager = minimumGroupFightManager;
        this.fightProvider = fightProvider;
        this.groupLinkProvider = groupLinkProvider;
        this.groupConverter = groupConverter;
        this.teamConverter = teamConverter;
    }

    @Override
    public List<Group> getGroups(Tournament tournament, Integer level) {
        return groupProvider.getGroups(tournament, level);
    }

    private int getNumberOfWinners(Tournament tournament) {
        final TournamentExtraProperty numberOfWinnersProperty = tournamentExtraPropertyProvider.getByTournamentAndProperty(tournament,
                TournamentExtraPropertyKey.NUMBER_OF_WINNERS);

        if (numberOfWinnersProperty != null) {
            try {
                return Integer.parseInt(numberOfWinnersProperty.getPropertyValue());
            } catch (Exception ignore) {

            }
        }
        return 1;
    }

    private boolean getMaxGroupFights(Tournament tournament) {
        final TournamentExtraProperty maximizeFightsProperty = tournamentExtraPropertyProvider.getByTournamentAndProperty(tournament,
                TournamentExtraPropertyKey.MAXIMIZE_FIGHTS);

        if (maximizeFightsProperty != null) {
            try {
                return Boolean.getBoolean(maximizeFightsProperty.getPropertyValue());
            } catch (Exception ignore) {

            }
        }
        return false;
    }

    @Override
    public Group addGroup(Tournament tournament, Group group) {
        if (group.getLevel() > 0) {
            throw new InvalidGroupException(this.getClass(), "Groups can only be added at level 0.");
        }
        final Group savedGroup = groupProvider.addGroup(tournament, group);
        adjustGroupsSize(tournament, getNumberOfWinners(tournament));
        return savedGroup;
    }

    public void adjustGroupsSize(Tournament tournament, int numberOfWinners) {
        //Check if inner levels must be increased on size.
        final List<Group> tournamentGroups = groupProvider.getGroups(tournament);
        final Map<Integer, List<Group>> groupsByLevel = GroupUtils.orderByLevel(tournamentGroups);
        int previousLevelSize = 0;
        for (final Integer level : new HashSet<>(groupsByLevel.keySet())) {
            if (groupsByLevel.get(level).size() < ((((previousLevelSize + 1) * (level == 1 ? numberOfWinners : 1))) / 2)) {
                final Group levelGroup = new Group(tournament, level, groupsByLevel.get(level).size());
                groupProvider.addGroup(tournament, levelGroup);
                groupsByLevel.get(level).add(levelGroup);
            }
            previousLevelSize = groupsByLevel.get(level).size();
        }

        //Add extra level if needed.
        if (groupsByLevel.get(groupsByLevel.size() - 1).size() > 1 || (groupsByLevel.size() == 1 && numberOfWinners > 1)) {
            final Integer newLevel = groupsByLevel.size();
            final Group levelGroup = new Group(tournament, newLevel, 0);
            groupsByLevel.put(newLevel, new ArrayList<>());
            groupsByLevel.get(newLevel).add(levelGroup);
            groupProvider.addGroup(tournament, levelGroup);
        }
    }

    @Override
    public void removeGroup(Tournament tournament, Integer groupLevel, Integer groupIndex) {
        if (groupLevel > 0) {
            throw new InvalidGroupException(this.getClass(), "Groups can only be deleted at level 0.");
        }

        groupProvider.deleteGroupByLevelAndIndex(tournament, groupLevel, groupIndex);
        final int numberOfWinners = getNumberOfWinners(tournament);


        //Check if inner levels must be decreased on size.
        final List<Group> tournamentGroups = groupProvider.getGroups(tournament);
        final Map<Integer, List<Group>> groupsByLevel = GroupUtils.orderByLevel(tournamentGroups);
        int previousLevelSize = Integer.MAX_VALUE - 1;
        for (final Integer level : new HashSet<>(groupsByLevel.keySet())) {
            // Normal levels, the number of groups must be the half rounded up that the previous one.
            if ((numberOfWinners == 1 || level > 1)
                    && (previousLevelSize == 1 || groupsByLevel.get(level).size() > ((previousLevelSize + 1) / 2))) {
                groupProvider.deleteGroupByLevelAndIndex(tournament, level, groupsByLevel.get(level).size() - 1);
                groupsByLevel.get(level).remove(groupsByLevel.get(level).size() - 1);
                // First level with 2 winners must have the same size that level zero.
            } else if (numberOfWinners == 2 && groupsByLevel.get(level).size() > previousLevelSize) {
                groupProvider.deleteGroupByLevelAndIndex(tournament, level, groupsByLevel.get(level).size() - 1);
                groupsByLevel.get(level).remove(groupsByLevel.get(level).size() - 1);
            }
            previousLevelSize = groupsByLevel.get(level).size();
        }
    }

    @Override
    public List<Fight> createFights(Tournament tournament, TeamsOrder teamsOrder, Integer level, String createdBy) {
        final List<Group> tournamentGroups = groupProvider.getGroups(tournament);
        final List<Fight> createdFights = new ArrayList<>();
        tournamentGroups.forEach(group -> {
            if (Objects.equals(group.getLevel(), level)) {
                final List<Fight> fights;
                if (getMaxGroupFights(tournament)) {
                    final TournamentExtraProperty extraProperty = getLeagueFightsOrder(tournament);
                    fights = fightProvider.saveAll(completeGroupFightManager.createFights(tournament, group.getTeams(),
                            TeamsOrder.NONE, level, LeagueFightsOrder.get(extraProperty.getPropertyValue()) == LeagueFightsOrder.FIFO, createdBy));
                } else {
                    fights = fightProvider.saveAll(minimumGroupFightManager.createFights(tournament, group.getTeams(),
                            TeamsOrder.NONE, level, createdBy));
                }
                group.setFights(fights);
                groupProvider.save(group);
                createdFights.addAll(fights);
            }
        });
        return createdFights;
    }

    private Integer getNextEmptyLevel(List<Group> tournamentGroups) {
        if (tournamentGroups == null) {
            return null;
        }
        for (Group group : tournamentGroups) {
            if (group.getTeams().isEmpty()) {
                return group.getLevel();
            }
        }
        return null;
    }

    private void populateLevel(Tournament tournament, int level) throws LevelNotFinishedException {
        final List<GroupLink> links = groupLinkProvider.generateLinks(tournament);
        final List<GroupLink> levelLinks = links.stream().filter(link -> link.getDestination().getLevel() == level).toList();
        final Set<Group> groupsOfLevel = new HashSet<>();
        for (GroupLink link : levelLinks) {
            final List<ScoreOfTeamDTO> teamsRanking = rankingController.getTeamsScoreRanking(
                    groupConverter.convert(new GroupConverterRequest(link.getSource())));
            checkDrawScore(link.getSource(), teamsRanking, link.getWinner());
            if (link.getWinner() != null && teamsRanking.get(link.getWinner()) != null && teamsRanking.get(link.getWinner()).getTeam() != null) {
                link.getDestination().getTeams().add(teamConverter.reverse(teamsRanking.get(link.getWinner()).getTeam()));
            } else {
                KendoTournamentLogger.warning(this.getClass(), "Missing data for level '' population with winner '' using ranking:\n\t",
                        level, link.getWinner(), link.getWinner() != null ? teamsRanking.get(link.getWinner()) : null);
            }
            groupsOfLevel.add(link.getDestination());
        }
        groupProvider.saveAll(groupsOfLevel);
    }

    private void checkDrawScore(Group group, List<ScoreOfTeamDTO> scoresOfTeamsDTO, int numberOfWinners) {
        for (int i = 0; i <= numberOfWinners; i++) {
            final int winner = i;
            final List<ScoreOfTeamDTO> sameLevelScore = scoresOfTeamsDTO.stream().filter(scoreOfTeamDTO -> scoreOfTeamDTO.getSortingIndex() == winner).toList();
            if (sameLevelScore.size() > 1) {
                KendoTournamentLogger.debug(this.getClass(), "Teams with same score are '{}'.", sameLevelScore.stream().map(ScoreOfTeamDTO::getTeam).toList());
                throw new LevelNotFinishedException(this.getClass(), "There is a draw value on winner '" + winner + "' on group '" + group + "'");
            }
        }
    }

    @Override
    public List<Fight> generateNextFights(Tournament tournament, String createdBy) {
        //Get next level to continue if exists.
        final List<Group> tournamentGroups = groupProvider.getGroups(tournament);
        if (tournamentGroups == null) {
            return null;
        }

        final Integer nextLevel = getNextEmptyLevel(tournamentGroups);
        if (nextLevel == null) {
            KendoTournamentLogger.debug(this.getClass(), "No next level to populate!");
            return new ArrayList<>();
        }

        //Populate the next level with winners.
        populateLevel(tournament, nextLevel);


        //Generate next Level fights.
        return createFights(tournament, TeamsOrder.NONE, nextLevel, createdBy);
    }
}
