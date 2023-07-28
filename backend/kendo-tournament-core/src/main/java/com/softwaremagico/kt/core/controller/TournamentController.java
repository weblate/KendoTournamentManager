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

import com.softwaremagico.kt.core.controller.models.TournamentDTO;
import com.softwaremagico.kt.core.converters.TournamentConverter;
import com.softwaremagico.kt.core.converters.models.TournamentConverterRequest;
import com.softwaremagico.kt.core.providers.DuelProvider;
import com.softwaremagico.kt.core.providers.FightProvider;
import com.softwaremagico.kt.core.providers.GroupProvider;
import com.softwaremagico.kt.core.providers.RoleProvider;
import com.softwaremagico.kt.core.providers.TeamProvider;
import com.softwaremagico.kt.core.providers.TournamentProvider;
import com.softwaremagico.kt.persistence.entities.Group;
import com.softwaremagico.kt.persistence.entities.Tournament;
import com.softwaremagico.kt.persistence.repositories.TournamentRepository;
import com.softwaremagico.kt.persistence.values.TournamentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class TournamentController extends BasicInsertableController<Tournament, TournamentDTO, TournamentRepository,
        TournamentProvider, TournamentConverterRequest, TournamentConverter> {

    private final GroupProvider groupProvider;

    private final TeamProvider teamProvider;

    private final RoleProvider roleProvider;

    private final FightProvider fightProvider;

    private final DuelProvider duelProvider;


    @Autowired
    public TournamentController(TournamentProvider provider, TournamentConverter converter, GroupProvider groupProvider, TeamProvider teamProvider,
                                RoleProvider roleProvider, FightProvider fightProvider, DuelProvider duelProvider) {
        super(provider, converter);
        this.groupProvider = groupProvider;
        this.teamProvider = teamProvider;
        this.roleProvider = roleProvider;
        this.fightProvider = fightProvider;
        this.duelProvider = duelProvider;
    }

    @Override
    protected TournamentConverterRequest createConverterRequest(Tournament entity) {
        return new TournamentConverterRequest(entity);
    }

    @Override
    public TournamentDTO create(TournamentDTO tournamentDTO, String username) {
        final TournamentDTO createdTournamentDTO = super.create(tournamentDTO, username);
        final Group group = new Group();
        group.setCreatedBy(username);
        groupProvider.addGroup(reverse(createdTournamentDTO), group);
        return createdTournamentDTO;
    }

    @Override
    public TournamentDTO update(TournamentDTO tournamentDTO, String username) {
        //If a tournament is locked we can define it as finished (maybe fights are not finished by time).
        if (tournamentDTO.isLocked() && tournamentDTO.getFinishedAt() == null) {
            tournamentDTO.setFinishedAt(LocalDateTime.now());
        }
        if (tournamentDTO.isLocked() && tournamentDTO.getLockedAt() == null) {
            tournamentDTO.setLockedAt(LocalDateTime.now());
        }
        return super.update(tournamentDTO, username);
    }

    public TournamentDTO create(String name, Integer shiaijos, Integer teamSize, TournamentType type, String username) {
        final TournamentDTO tournamentDTO = convert(getProvider().save(new Tournament(name, shiaijos != null ? shiaijos : 1,
                teamSize != null ? teamSize : TournamentProvider.DEFAULT_TEAM_SIZE, type != null ? type : TournamentType.LEAGUE, username)));
        //Add default group:
        final Group group = new Group();
        group.setCreatedBy(username);
        groupProvider.addGroup(reverse(tournamentDTO), group);
        return tournamentDTO;
    }

    @Override
    public void deleteById(Integer id) {
        delete(get(id));
    }

    @Override
    public void delete(TournamentDTO entity) {
        final Tournament tournament = reverse(entity);
        groupProvider.delete(tournament);
        fightProvider.delete(tournament);
        duelProvider.delete(tournament);
        teamProvider.delete(tournament);
        roleProvider.delete(tournament);
        getProvider().delete(tournament);
    }

    public List<TournamentDTO> getPreviousTo(TournamentDTO tournamentDTO, int elementsToRetrieve) {
        return convertAll(getProvider().getPreviousTo(reverse(tournamentDTO), elementsToRetrieve));
    }
}
