import { BrowserModule } from '@angular/platform-browser';
import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { AngularMaterialModule } from './angular-material.module';
import { EhrsComponent } from './ehrs/ehrs.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { EhrFormComponent } from './ehr-form/ehr-form.component';
import { MatTableModule } from '@angular/material/table';
import { EhrService } from './services/ehr.service';
import { NotificationService } from './services/notification.service';
import { HttpClientModule } from '@angular/common/http';
import { OriginActionFormComponent } from './origin-action-form/origin-action-form.component';

@NgModule({
  declarations: [
    AppComponent,
    EhrsComponent,
    EhrFormComponent,
    OriginActionFormComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    AngularMaterialModule,
    FormsModule,
    MatTableModule,
    HttpClientModule,
    ReactiveFormsModule
  ],
  providers: [
    EhrService,
    NotificationService
  ],
  bootstrap: [AppComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule { }
