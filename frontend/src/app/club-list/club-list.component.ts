import {Component, ViewChild, OnInit} from '@angular/core';
import {MatPaginator} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';
import {MatTable} from '@angular/material/table';
import {MatSort} from '@angular/material/sort';
import {MatDialog} from '@angular/material/dialog';
import {SelectionModel} from "@angular/cdk/collections";
import {ClubService} from '../services/club.service';
import {Club} from '../models/club';
import {ClubDialogBoxComponent} from './club-dialog-box/club-dialog-box.component';
import {Action} from './club-dialog-box/club-dialog-box.component';
import {MessageService} from "../services/message.service";


@Component({
  selector: 'app-club-list',
  templateUrl: './club-list.component.html',
  styleUrls: ['./club-list.component.scss']
})
export class ClubListComponent implements OnInit {

  columns: string[] = ['id', 'name', 'country', 'city', 'address', 'email', 'phone', 'web'];
  columnsTags: string[] = ['idHeader', 'nameHeader', 'countryHeader', 'cityHeader', 'addressHeader', 'emailHeader', 'phoneHeader', 'webHeader'];
  visibleColumns: string[] = ['name', 'country', 'city'];
  selection = new SelectionModel<Club>(false, []);
  dataSource: MatTableDataSource<Club>;
  selectedElement: Club | undefined;

  @ViewChild(MatPaginator, {static: true}) paginator!: MatPaginator;
  @ViewChild(MatTable, {static: true}) table: MatTable<any>;
  @ViewChild(MatSort, {static: true}) sort!: MatSort;

  constructor(private clubService: ClubService, public dialog: MatDialog, private messageService: MessageService) {
  }

  ngOnInit(): void {
    this.showAllElements();
    this.dataSource = new MatTableDataSource<Club>();
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  showAllElements(): void {
    this.clubService.getAll().subscribe(clubs => {
      this.dataSource.data = clubs;
    });
  }

  addElement(): void {
    let club = new Club();
    this.openDialog('Add a new club', Action.Add, club);
  }

  editElement(): void {
    if (this.selectedElement) {
      this.openDialog('Edit club', Action.Update, this.selectedElement);
    }
  }

  deleteElement(): void {
    if (this.selectedElement) {
      this.openDialog('Delete club', Action.Delete, this.selectedElement);
    }
  }

  setSelectedItem(row: Club): void {
    if (row === this.selectedElement) {
      this.selectedElement = undefined;
    } else {
      this.selectedElement = row;
    }
  }

  openDialog(title: string, action: Action, club: Club) {
    const dialogRef = this.dialog.open(ClubDialogBoxComponent, {
      width: '250px',
      data: {title: title, action: action, entity: club}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result.action == Action.Add) {
        this.addRowData(result.data);
      } else if (result.action == Action.Update) {
        this.updateRowData(result.data);
      } else if (result.action == Action.Delete) {
        this.deleteRowData(result.data);
      }
    });
  }

  addRowData(club: Club) {
    this.clubService.add(club).subscribe(club => {
      this.dataSource.data.push(club);
      this.dataSource._updateChangeSubscription();
      this.messageService.infoMessage("clubStored");
    });
  }

  updateRowData(club: Club) {
    this.clubService.update(club).subscribe(club => {
        this.messageService.infoMessage("clubUpdated");
      }
    );
  }

  deleteRowData(club: Club) {
    this.clubService.delete(club).subscribe(n => {
        this.dataSource.data = this.dataSource.data.filter(existing_club => existing_club !== club);
        this.messageService.infoMessage("clubDeleted");
      }
    );
  }

  filter(event: Event) {
    const filter = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filter.trim().toLowerCase();
  }

  isColumnVisible(column: string): boolean {
    return this.visibleColumns.includes(column);
  }

  toggleColumnVisibility(column: string) {
    const index: number = this.visibleColumns.indexOf(column);
    if (index !== -1) {
      this.visibleColumns.splice(index, 1);
    } else {
      let oldVisibleColumns: string[];
      oldVisibleColumns = [...this.visibleColumns];
      oldVisibleColumns.push(column);
      this.visibleColumns.length = 0;
      //Maintain columns order.
      for (let column of this.columns) {
        if (oldVisibleColumns.includes(column)) {
          this.visibleColumns.push(column);
        }
      }
    }
  }

}
