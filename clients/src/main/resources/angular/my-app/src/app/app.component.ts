import { Component, ViewChild } from '@angular/core';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { EhrFormComponent } from './ehr-form/ehr-form.component';
import { EhrsComponent } from './ehrs/ehrs.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'my-app';

  @ViewChild(EhrsComponent) private ehrComponent: EhrsComponent;

  constructor() { }

}
