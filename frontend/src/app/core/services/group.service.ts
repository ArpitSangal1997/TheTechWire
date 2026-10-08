import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
export interface GroupMember { id: number; displayName: string; handle: string; role: string; }
export interface GroupPostComment { id: number; body: string; authorId: number; authorName: string; authorHandle: string; createdAt: string; replies: GroupPostComment[]; }
export interface GroupPost { id: number; title: string; body: string; authorId: number; authorName: string; authorHandle: string; createdAt: string; groupId: number; groupName: string; trail?: { id: number; title: string; slug: string }; commentCount: number; comments: GroupPostComment[]; }
export type GroupVisibility = 'PUBLIC' | 'PRIVATE';
export type GroupJoinPolicy = 'OPEN' | 'REQUEST' | 'INVITE_ONLY';
export interface Group { id: number; name: string; slug: string; description?: string; creatorId: number; creatorName: string; creatorHandle: string; createdAt: string; memberCount: number; visibility: GroupVisibility; joinPolicy: GroupJoinPolicy; topics: string[]; rules?: string; isMember: boolean; canManage: boolean; joinRequestPending: boolean; }
export interface GroupJoinRequest { id: number; userId: number; displayName: string; handle: string; status: string; createdAt: string; }
export interface GroupDetail extends Group { posts: GroupPost[]; members: GroupMember[]; canFollow: boolean; canRequestJoin: boolean; joinRequests: GroupJoinRequest[]; }
@Injectable({providedIn:'root'}) export class GroupService {
 constructor(private http:HttpClient) {}
 list():Observable<Group[]> { return this.http.get<Group[]>(`${environment.apiUrl}/groups`); }
 detail(id:number):Observable<GroupDetail> { return this.http.get<GroupDetail>(`${environment.apiUrl}/groups/${id}`); }
 create(payload:{name:string;description:string;visibility:GroupVisibility;joinPolicy:GroupJoinPolicy;topics:string[];rules:string}):Observable<Group> { return this.http.post<Group>(`${environment.apiUrl}/groups`,payload); }
 update(id:number,payload:{visibility:GroupVisibility;joinPolicy:GroupJoinPolicy;description:string;topics:string[];rules:string}):Observable<GroupDetail> { return this.http.patch<GroupDetail>(`${environment.apiUrl}/groups/${id}`,payload); }
 follow(groupId:number):Observable<GroupDetail> { return this.http.post<GroupDetail>(`${environment.apiUrl}/groups/${groupId}/follow`,{}); }
 requestToJoin(groupId:number):Observable<GroupDetail> { return this.http.post<GroupDetail>(`${environment.apiUrl}/groups/${groupId}/join-requests`,{}); }
 decideJoinRequest(groupId:number,requestId:number,status:'APPROVED'|'DECLINED'):Observable<GroupDetail> { return this.http.patch<GroupDetail>(`${environment.apiUrl}/groups/${groupId}/join-requests/${requestId}`,{status}); }
 addMember(groupId:number,userId:number):Observable<GroupDetail> { return this.http.post<GroupDetail>(`${environment.apiUrl}/groups/${groupId}/members`,{userId}); }
 removeMember(groupId:number,userId:number):Observable<void> { return this.http.delete<void>(`${environment.apiUrl}/groups/${groupId}/members/${userId}`); }
 createPost(groupId:number,title:string,body:string,trailTitle:string):Observable<GroupPost> { return this.http.post<GroupPost>(`${environment.apiUrl}/groups/${groupId}/posts`,{title,body,trailTitle}); }
 addComment(groupId:number,postId:number,body:string,parentId?:number):Observable<GroupPost> { return this.http.post<GroupPost>(`${environment.apiUrl}/groups/${groupId}/posts/${postId}/comments`,{body,parentId}); }
 deleteComment(groupId:number,postId:number,commentId:number):Observable<void> { return this.http.delete<void>(`${environment.apiUrl}/groups/${groupId}/posts/${postId}/comments/${commentId}`); }
 deletePost(groupId:number,postId:number):Observable<void> { return this.http.delete<void>(`${environment.apiUrl}/groups/${groupId}/posts/${postId}`); }
 delete(id:number):Observable<void> { return this.http.delete<void>(`${environment.apiUrl}/groups/${id}`); }
}
