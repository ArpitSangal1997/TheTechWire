import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Group, GroupJoinPolicy, GroupService, GroupVisibility } from '../../core/services/group.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
	selector: 'wb-groups',
	standalone: true,
	imports: [CommonModule, FormsModule, RouterModule],
	templateUrl: './groups.component.html',
	styleUrl: './groups.component.scss'
})
export class GroupsComponent implements OnInit {
	groups: Group[] = [];
	name = '';
	description = '';
	visibility: GroupVisibility = 'PUBLIC';
	joinPolicy: GroupJoinPolicy = 'OPEN';
	topics = '';
	rules = '';
	error = '';

	constructor(private service: GroupService, public auth: AuthService) {}

	ngOnInit(): void { this.load(); }

	visibilityChanged(): void {
		this.joinPolicy = this.visibility === 'PUBLIC' ? 'OPEN' : 'INVITE_ONLY';
	}

	load(): void {
		this.service.list().subscribe({
			next: groups => { this.groups = groups; this.error = ''; },
			error: () => this.error = 'Groups could not be loaded.'
		});
	}

	create(): void {
		if (!this.name.trim() || !this.auth.isLoggedIn()) return;
		this.service.create({
			name: this.name.trim(),
			description: this.description.trim(),
			visibility: this.visibility,
			joinPolicy: this.visibility === 'PUBLIC' ? 'OPEN' : this.joinPolicy,
			topics: this.topics.split(',').map(topic => topic.trim()).filter(Boolean),
			rules: this.rules.trim()
		}).subscribe({
			next: () => {
				this.name = '';
				this.description = '';
				this.topics = '';
				this.rules = '';
				this.visibility = 'PUBLIC';
				this.joinPolicy = 'OPEN';
				this.load();
			},
			error: err => this.error = err?.error?.message ?? 'Group could not be created.'
		});
	}

	follow(group: Group): void {
		this.service.follow(group.id).subscribe({
			next: () => this.load(),
			error: err => this.error = err?.error?.message ?? 'Could not follow this group.'
		});
	}

	requestToJoin(group: Group): void {
		this.service.requestToJoin(group.id).subscribe({
			next: () => this.load(),
			error: err => this.error = err?.error?.message ?? 'Could not request access to this group.'
		});
	}

	remove(group: Group): void {
		if (!confirm(`Delete ${group.name}?`)) return;
		this.service.delete(group.id).subscribe({
			next: () => this.load(),
			error: err => this.error = err?.error?.message ?? 'Group could not be deleted.'
		});
	}
}
