import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { EhrsComponent } from './ehrs/ehrs.component';
import { EhrFormComponent } from './ehr-form/ehr-form.component';


const routes: Routes = [
  {path:'', redirectTo: '/form', pathMatch: 'full'},
  {path:'form', component: EhrFormComponent},
  {path:'origin', component: EhrsComponent},
  {path:'target', component: EhrsComponent},
  {path:'patient', component: EhrsComponent}
];
export const appRouting = RouterModule.forRoot(routes);


@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
