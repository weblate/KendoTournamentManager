import {Component, OnInit} from '@angular/core';
import {Router} from "@angular/router";
import {RbacService} from "../../services/rbac/rbac.service";
import {SystemOverloadService} from "../../services/notifications/system-overload.service";
import {RbacBasedComponent} from "../../components/RbacBasedComponent";
import {ParticipantStatistics} from "../../models/participant-statistics.model";
import {DatePipe} from "@angular/common";
import {UserSessionService} from "../../services/user-session.service";
import {RoleType} from "../../models/role-type";
import {convertDate, convertSeconds} from "../../utils/dates/date-conversor";
import {StatisticsService} from "../../services/statistics.service";
import {PieChartData} from "../../components/charts/pie-chart/pie-chart-data";
import {Score} from "../../models/score";
import {TranslateService} from "@ngx-translate/core";
import {truncate} from "../../utils/maths/truncate";
import {GaugeChartData} from "../../components/charts/gauge-chart/gauge-chart-data";
import {RankingService} from "../../services/ranking.service";
import {CompetitorRanking} from "../../models/competitor-ranking";
import {AchievementsService} from "../../services/achievements.service";
import {Achievement} from "../../models/achievement.model";

@Component({
  selector: 'app-participant-statistics',
  templateUrl: './participant-statistics.component.html',
  styleUrls: ['./participant-statistics.component.scss']
})
export class ParticipantStatisticsComponent extends RbacBasedComponent implements OnInit {

  pipe: DatePipe;

  private readonly participantId: number | undefined;
  public participantStatistics: ParticipantStatistics | undefined = undefined;
  public roleTypes: RoleType[] = RoleType.toArray();
  public competitorRanking: CompetitorRanking;

  public hitsTypeChartData: PieChartData;
  public receivedHitsTypeChartData: PieChartData;
  public performanceRadialData: GaugeChartData;
  public performance: [string, number][];
  public achievements: Achievement[];

  constructor(private router: Router, rbacService: RbacService, private systemOverloadService: SystemOverloadService,
              private userSessionService: UserSessionService, private statisticsService: StatisticsService,
              private translateService: TranslateService, private rankingService: RankingService,
              private achievementService: AchievementsService) {
    super(rbacService);
    let state = this.router.getCurrentNavigation()?.extras.state;
    if (state) {
      if (state['participantId'] && !isNaN(Number(state['participantId']))) {
        this.participantId = Number(state['participantId']);
      } else {
        this.goBackToUsers();
      }
    } else {
      this.goBackToUsers();
    }
    this.setLocale();
  }

  private setLocale(): void {
    if (this.userSessionService.getLanguage() === 'es' || this.userSessionService.getLanguage() === 'ca') {
      this.pipe = new DatePipe('es');
    } else if (this.userSessionService.getLanguage() === 'it') {
      this.pipe = new DatePipe('it');
    } else if (this.userSessionService.getLanguage() === 'de') {
      this.pipe = new DatePipe('de');
    } else if (this.userSessionService.getLanguage() === 'nl') {
      this.pipe = new DatePipe('nl');
    } else {
      this.pipe = new DatePipe('en-US');
    }
  }

  ngOnInit(): void {
    this.generateStatistics();
  }

  generateStatistics(): void {
    this.systemOverloadService.isTransactionalBusy.next(true);
    this.rankingService.getCompetitorsRanking(this.participantId!).subscribe((_competitorRanking: CompetitorRanking): void => {
      this.competitorRanking = _competitorRanking;
    })
    this.statisticsService.getParticipantStatistics(this.participantId!).subscribe((_participantStatistics: ParticipantStatistics): void => {
      this.participantStatistics = ParticipantStatistics.clone(_participantStatistics);
      this.initializeScoreStatistics(this.participantStatistics);
      this.systemOverloadService.isTransactionalBusy.next(false);
    });
    this.achievementService.getParticipantAchievements(this.participantId!).subscribe((_achievements: Achievement[]): void => {
      this.achievements = _achievements;
    });
  }

  initializeScoreStatistics(participantStatistics: ParticipantStatistics): void {
    this.hitsTypeChartData = PieChartData.fromArray(this.obtainPoints(participantStatistics));
    this.receivedHitsTypeChartData = PieChartData.fromArray(this.obtainReceivedPoints(participantStatistics));
    this.performance = this.generatePerformanceStatistics(participantStatistics);
    this.performanceRadialData = GaugeChartData.fromArray(this.performance);
  }

  generatePerformanceStatistics(participantStatistics: ParticipantStatistics): [string, number][] {
    const performance: [string, number][] = [];
    performance.push(['attack', truncate(participantStatistics.participantFightStatistics.getTotalHits() / (participantStatistics.participantFightStatistics.duelsNumber * 2) * 100, 2)]);
    performance.push(['defense', truncate((1 - (participantStatistics.participantFightStatistics.getTotalReceivedHits() / (participantStatistics.participantFightStatistics.duelsNumber * 2))) * 100, 2)]);
    performance.push(['willpower', participantStatistics.totalTournaments > 0 ?
      (participantStatistics.tournaments / participantStatistics.totalTournaments) * 100 : 0]);
    const aggressivenessMargin: number = 20;
    performance.push(['aggressiveness', participantStatistics.participantFightStatistics.averageTime > 0 ?
      Math.min(100, truncate((1 - ((participantStatistics.participantFightStatistics.averageTime - aggressivenessMargin) / 180)) * 100, 2)) : 0]);
    return performance;
  }

  obtainPoints(participantStatistics: ParticipantStatistics): [string, number][] {
    const scores: [string, number][] = [];
    if (participantStatistics?.participantFightStatistics) {
      scores.push([Score.label(Score.MEN), participantStatistics.participantFightStatistics.menNumber ? participantStatistics.participantFightStatistics.menNumber : 0]);
      scores.push([Score.label(Score.KOTE), participantStatistics.participantFightStatistics.koteNumber ? participantStatistics.participantFightStatistics.koteNumber : 0]);
      scores.push([Score.label(Score.DO), participantStatistics.participantFightStatistics.doNumber ? participantStatistics.participantFightStatistics.doNumber : 0]);
      scores.push([Score.label(Score.TSUKI), participantStatistics.participantFightStatistics.tsukiNumber ? participantStatistics.participantFightStatistics.tsukiNumber : 0]);
      scores.push([Score.label(Score.IPPON), participantStatistics.participantFightStatistics.ipponNumber ? participantStatistics.participantFightStatistics.ipponNumber : 0]);
    }
    return scores;
  }

  obtainReceivedPoints(participantStatistics: ParticipantStatistics): [string, number][] {
    const scores: [string, number][] = [];
    if (participantStatistics?.participantFightStatistics) {
      scores.push([Score.label(Score.MEN), participantStatistics.participantFightStatistics.receivedMenNumber ? participantStatistics.participantFightStatistics.receivedMenNumber : 0]);
      scores.push([Score.label(Score.KOTE), participantStatistics.participantFightStatistics.receivedKoteNumber ? participantStatistics.participantFightStatistics.receivedKoteNumber : 0]);
      scores.push([Score.label(Score.DO), participantStatistics.participantFightStatistics.receivedDoNumber ? participantStatistics.participantFightStatistics.receivedDoNumber : 0]);
      scores.push([Score.label(Score.TSUKI), participantStatistics.participantFightStatistics.receivedTsukiNumber ? participantStatistics.participantFightStatistics.receivedTsukiNumber : 0]);
      scores.push([Score.label(Score.IPPON), participantStatistics.participantFightStatistics.receivedIpponNumber ? participantStatistics.participantFightStatistics.receivedIpponNumber : 0]);
    }
    return scores;
  }

  goBackToUsers(): void {
    this.router.navigate(['/participants'], {});
  }

  numberOfPerformedRoles(roleType: RoleType): number {
    if (this.participantStatistics !== undefined) {
      return this.participantStatistics.numberOfRolePerformed(roleType);
    }
    return 0;
  }

  convertSeconds(seconds: number | undefined): string {
    return convertSeconds(seconds);
  }

  convertDate(date: Date | undefined): string | null {
    return convertDate(this.pipe, date);
  }
}
