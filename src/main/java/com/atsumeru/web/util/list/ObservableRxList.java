package com.atsumeru.web.util.list;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Deprecated
public class ObservableRxList<T> {
    protected final List<T> list;
    protected final PublishSubject<List<T>> subject;

    public ObservableRxList() {
        this.list = new ArrayList<>();
        this.subject = PublishSubject.create();
    }

    public void add(T value) {
        list.add(value);
        subject.onNext(list);
    }

    public void addAll(List<T> value) {
        list.addAll(value);
        subject.onNext(list);
    }

    public void update(T value) {
        for (ListIterator<T> it = list.listIterator(); it.hasNext(); ) {
            if (value == it.next()) {
                it.set(value);
                break;
            }
        }
        subject.onNext(list);
    }

    public void update(int position, T value) {
        list.set(position, value);
        subject.onNext(list);
    }

    public void remove(T value) {
        list.remove(value);
        subject.onNext(list);
    }

    public void remove(int index) {
        list.remove(index);
        subject.onNext(list);
    }

    public Observable<List<T>> getObservable() {
        return subject;
    }

    public List<T> getCurrentList() {
        return list;
    }

    public int size() {
        return list.size();
    }

    public void clear() {
        list.clear();
    }
}
