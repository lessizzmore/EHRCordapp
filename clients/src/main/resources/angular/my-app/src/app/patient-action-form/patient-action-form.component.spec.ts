import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PatientActionFormComponent } from './patient-action-form.component';

describe('PatientActionFormComponent', () => {
  let component: PatientActionFormComponent;
  let fixture: ComponentFixture<PatientActionFormComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PatientActionFormComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PatientActionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
