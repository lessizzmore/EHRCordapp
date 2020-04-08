import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TargetEhrComponent } from './target-ehr.component';

describe('TargetEhrComponent', () => {
  let component: TargetEhrComponent;
  let fixture: ComponentFixture<TargetEhrComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TargetEhrComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TargetEhrComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
