import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { OriginActionFormComponent } from './origin-action-form.component';

describe('OriginActionFormComponent', () => {
  let component: OriginActionFormComponent;
  let fixture: ComponentFixture<OriginActionFormComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ OriginActionFormComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OriginActionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
