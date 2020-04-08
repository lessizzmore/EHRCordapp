import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { EhrsComponent } from './ehrs/ehrs.component';
import { EhrFormComponent } from './ehr-form/ehr-form.component';
import { PatientEhrComponent } from './patient-ehr/patient-ehr.component';
import { TargetEhrComponent } from './target-ehr/target-ehr.component';


const routes: Routes = [
  {path:'', redirectTo: '/form', pathMatch: 'full'},
  {path:'form', component: EhrFormComponent},
  {path:'origin', component: EhrsComponent},
  {path:'target', component: TargetEhrComponent},
  {path:'patient', component: PatientEhrComponent}
];
export const appRouting = RouterModule.forRoot(routes);


@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
