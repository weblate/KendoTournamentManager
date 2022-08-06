import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {BasicTableData} from "./basic-table-data";
import {MatDialog} from "@angular/material/dialog";
import {MatPaginator, PageEvent} from '@angular/material/paginator';
import {MatSort} from "@angular/material/sort";
import {UserSessionService} from "../../../services/user-session.service";
import {TranslateService} from "@ngx-translate/core";
import {DatePipe} from "@angular/common";

@Component({
  selector: 'basic-table',
  templateUrl: './basic-table.component.html',
  styleUrls: ['./basic-table.component.scss']
})
export class BasicTableComponent implements OnInit {

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  @Input()
  basicTableData: BasicTableData<any>;

  pipe: DatePipe;

  constructor(public dialog: MatDialog, private userSession: UserSessionService, private translateService: TranslateService,
              private userSessionService: UserSessionService) {
    this.setLocale();
  }

  ngOnInit(): void {
    // This is intentional
  }

  ngAfterViewInit() {
    this.basicTableData.dataSource.paginator = this.paginator;
    this.basicTableData.dataSource.sort = this.sort;
  }

  private setLocale() {
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

  setSelectedItem(row: any): void {
    if (row === this.basicTableData.selectedElement) {
      this.basicTableData.selectedElement = undefined;
    } else {
      this.basicTableData.selectedElement = row;
    }
  }

  filter(event: Event) {
    const filter = (event.target as HTMLInputElement).value;
    this.basicTableData.dataSource.filter = filter.trim().toLowerCase();
  }

  isColumnVisible(column: string): boolean {
    return this.basicTableData.visibleColumns.includes(column);
  }

  toggleColumnVisibility(column: string) {
    const index: number = this.basicTableData.visibleColumns.indexOf(column);
    if (index !== -1) {
      this.basicTableData.visibleColumns.splice(index, 1);
    } else {
      let oldVisibleColumns: string[];
      oldVisibleColumns = [...this.basicTableData.visibleColumns];
      oldVisibleColumns.push(column);
      this.basicTableData.visibleColumns.length = 0;
      //Maintain columns order.
      for (let tableColumn of this.basicTableData.columns) {
        if (oldVisibleColumns.includes(tableColumn)) {
          this.basicTableData.visibleColumns.push(tableColumn);
        }
      }
    }
  }

  getDefaultPageSize(): number {
    return this.userSession.getItemsPerPage();
  }


  onPaginateChange($event: PageEvent) {
    this.userSession.setItemsPerPage($event.pageSize);
  }

  getColumnData(column: any): any {
    if (typeof column === 'number') {
      return column;
    } else if (!isNaN(Date.parse(column))) {
      return this.pipe.transform(column, 'short');
    } else {
      if (column) {
        return this.translateService.instant(column);
      } else {
        return "";
      }
    }
  }
}
