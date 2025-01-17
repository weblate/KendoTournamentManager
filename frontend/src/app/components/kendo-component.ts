import {Component, OnDestroy} from "@angular/core";
import {Subject} from "rxjs";

@Component({
  template: ''
})
export abstract class KendoComponent implements OnDestroy {
  destroySubject = new Subject<boolean>();

  ngOnDestroy(): void {
    this.destroySubject.next(true);
    this.destroySubject.complete();
  }
}
