import { BrowserModule } from '@angular/platform-browser';
import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { AngularMaterialModule } from './angular-material.module';
import { EhrsComponent } from './ehrs/ehrs.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { EhrFormComponent } from './ehr-form/ehr-form.component';
import { EhrService } from './services/ehr.service';
import { NotificationService } from './services/notification.service';
import { HttpClientModule } from '@angular/common/http';
import { OriginActionFormComponent } from './origin-action-form/origin-action-form.component';
import { PatientActionFormComponent } from './patient-action-form/patient-action-form.component';
import { PatientEhrComponent } from './patient-ehr/patient-ehr.component';
import { TargetEhrComponent } from './target-ehr/target-ehr.component';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressBarModule} from '@angular/material/progress-bar';




@NgModule({
  declarations: [
    AppComponent,
    EhrsComponent,
    EhrFormComponent,
    OriginActionFormComponent,
    PatientActionFormComponent,
    PatientEhrComponent,
    TargetEhrComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    AngularMaterialModule,
    FormsModule,
    MatTableModule,
    HttpClientModule,
    ReactiveFormsModule,
    MatToolbarModule,  
    MatIconModule,  
    MatButtonModule,  
    MatCardModule,  
    MatProgressBarModule 
  ],
  providers: [
    EhrService,
    NotificationService
  ],
  bootstrap: [AppComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule { }
