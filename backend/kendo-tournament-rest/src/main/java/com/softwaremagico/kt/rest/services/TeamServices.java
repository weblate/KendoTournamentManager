package com.softwaremagico.kt.rest.services;

/*-
 * #%L
 * Kendo Tournament Manager (Rest)
 * %%
 * Copyright (C) 2021 - 2022 Softwaremagico
 * %%
 * This software is designed by Jorge Hortelano Otero. Jorge Hortelano Otero
 * <softwaremagico@gmail.com> Valencia (Spain).
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.softwaremagico.kt.core.providers.ParticipantProvider;
import com.softwaremagico.kt.core.providers.TeamProvider;
import com.softwaremagico.kt.core.providers.TournamentProvider;
import com.softwaremagico.kt.persistence.entities.Participant;
import com.softwaremagico.kt.persistence.entities.Team;
import com.softwaremagico.kt.persistence.entities.Tournament;
import com.softwaremagico.kt.rest.exceptions.BadRequestException;
import com.softwaremagico.kt.rest.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teams")
public class TeamServices {
    private final ParticipantProvider participantProvider;
    private final TeamProvider teamProvider;
    private final TournamentProvider tournamentProvider;
    private final ModelMapper modelMapper;

    public TeamServices(ParticipantProvider participantProvider, TeamProvider teamProvider, TournamentProvider tournamentProvider, ModelMapper modelMapper) {
        this.participantProvider = participantProvider;
        this.teamProvider = teamProvider;
        this.tournamentProvider = tournamentProvider;
        this.modelMapper = modelMapper;
    }

    @PreAuthorize("hasRole('ROLE_VIEWER')")
    @Operation(summary = "Gets all teams.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Team> getAll(HttpServletRequest request) {
        return teamProvider.getAll();
    }

    @PreAuthorize("hasRole('ROLE_VIEWER')")
    @Operation(summary = "Gets all teams.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/tournaments/{tournamentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Team> getAll(@Parameter(description = "Id of an existing tournament", required = true) @PathVariable("tournamentId") Integer tournamentId,
                             HttpServletRequest request) {
        return teamProvider.getAll(tournamentProvider.get(tournamentId));
    }

    @PreAuthorize("hasRole('ROLE_VIEWER')")
    @Operation(summary = "Gets all teams.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/tournaments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Team> getAll(@RequestBody TournamentDto tournamentDto,
                             HttpServletRequest request) {
        return teamProvider.getAll(modelMapper.map(tournamentDto, Tournament.class));
    }

    @PreAuthorize("hasRole('ROLE_VIEWER')")
    @Operation(summary = "Gets a team.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Team get(@Parameter(description = "Id of an existing team", required = true) @PathVariable("id") Integer id,
                    HttpServletRequest request) {
        return teamProvider.get(id);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Creates a team.", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Team add(@RequestBody TeamDto teamDto, HttpServletRequest request) {
        if (teamDto == null || teamDto.getTournament() == null) {
            throw new BadRequestException(getClass(), "Team data is missing");
        }
        final Tournament tournament = tournamentProvider.get(teamDto.getTournament().getId());
        if (tournament == null) {
            throw new BadRequestException(getClass(), "Selected tournament is wrong");
        }
        final Team team = new Team();
        if (teamDto.getName() != null) {
            team.setName(teamDto.getName());
        } else {
            team.setName(teamProvider.getNextDefaultName(tournament));
        }
        if (teamDto.getMembers() != null) {
            team.setMembers(participantProvider.get(teamDto.getMembers().stream().map(ParticipantDto::getId)
                    .collect(Collectors.toList())));
        }
        team.setTournament(tournament);
        if (teamDto.getGroup() != null) {
            team.setGroup(teamDto.getGroup());
        } else {
            team.setGroup(1);
        }
        return teamProvider.save(team);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Deletes a team.", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void delete(@Parameter(description = "Id of an existing team", required = true) @PathVariable("id") Integer id,
                       HttpServletRequest request) {
        teamProvider.delete(id);
    }

    @PreAuthorize("hasRole('ROLE_VIEWER')")
    @Operation(summary = "Gets all teams.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public void delete(@RequestBody TeamDto teamDto, HttpServletRequest request) {
        teamProvider.delete(modelMapper.map(teamDto, Team.class));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Deletes a member from any team.", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(value = "/delete/members", produces = MediaType.APPLICATION_JSON_VALUE)
    public Team delete(@RequestBody ParticipantInTournamentDto participantInTournament, HttpServletRequest request) {
        final Participant member = modelMapper.map(participantInTournament.getParticipant(), Participant.class);
        final Tournament tournament = modelMapper.map(participantInTournament.getTournament(), Tournament.class);
        return teamProvider.delete(tournament, member);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Deletes multiples member from any team.", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(value = "/delete/members/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public void delete(@RequestBody ParticipantsInTournamentDto participantsInTournaments, HttpServletRequest request) {
        for (final ParticipantDto participantInTournament : participantsInTournaments.getParticipant()) {
            final Participant member = modelMapper.map(participantInTournament, Participant.class);
            final Tournament tournament = modelMapper.map(participantsInTournaments.getTournament(), Tournament.class);
            teamProvider.delete(tournament, member);
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Deletes all teams from a tournament.", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(value = "/delete/tournaments", produces = MediaType.APPLICATION_JSON_VALUE)
    public void delete(@RequestBody TournamentDto tournamentDto, HttpServletRequest request) {
        teamProvider.delete(modelMapper.map(tournamentDto, Tournament.class));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Updates a team.", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Team update(@RequestBody TeamDto teamDto, HttpServletRequest request) {
        final Team team = modelMapper.map(teamDto, Team.class);
        Tournament tournament = null;
        if (teamDto.getTournament() != null) {
            tournament = tournamentProvider.get(teamDto.getTournament().getId());
            team.setTournament(tournament);
        }
        if (teamDto.getGroup() != null) {
            team.setGroup(teamDto.getGroup());
        }
        //Remove old members
        final List<Participant> members = new ArrayList<>();
        if (teamDto.getMembers() != null) {
            members.addAll(participantProvider.getOriginalOrder(teamDto.getMembers().stream().map(ParticipantDto::getId)
                    .collect(Collectors.toList())));
        }
        final Team storedTeam = teamProvider.update(team, members);
        if (tournament != null) {
            storedTeam.setTournament(tournament);
        }
        return storedTeam;
    }
}