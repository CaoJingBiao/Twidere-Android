/*
 *             Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.view.behavior.userprofile

import android.content.Context
import android.graphics.Rect
import android.support.design.widget.AccessorHeaderScrollingViewBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.lastWindowInsetsCompat
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.header_user.view.*
import org.mariotaku.twidere.R
import org.mariotaku.twidere.extension.view.measureChildIgnoringInsets

internal class PagerBehavior(context: Context, attrs: AttributeSet? = null) : AccessorHeaderScrollingViewBehavior(context, attrs) {

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        return dependency.id == R.id.profileHeader
    }

    override fun onMeasureChild(parent: CoordinatorLayout, child: View,
            parentWidthMeasureSpec: Int, widthUsed: Int, parentHeightMeasureSpec: Int,
            heightUsed: Int): Boolean {
        val topInset = parent.lastWindowInsetsCompat?.systemWindowInsetTop ?: 0
        return parent.measureChildIgnoringInsets(child, parentWidthMeasureSpec, widthUsed,
                parentHeightMeasureSpec - topInset, heightUsed)
    }

    override fun layoutChild(parent: CoordinatorLayout, child: View, layoutDirection: Int) {
        val header = parent.getDependencies(child).first()
        val lp = child.layoutParams as CoordinatorLayout.LayoutParams
        val rect = tempRect1
        rect.set(parent.paddingLeft + lp.leftMargin,
                header.contentBottom + lp.topMargin,
                parent.width - parent.paddingRight - lp.rightMargin,
                parent.height + header.contentBottom
                        - parent.paddingBottom - lp.bottomMargin)

        val parentInsets = parent.lastWindowInsetsCompat
        if ((parentInsets != null && ViewCompat.getFitsSystemWindows(parent)
                && !ViewCompat.getFitsSystemWindows(child))) {
            // If we're set to handle insets but this child isn't, then it has been measured as
            // if there are no insets. We need to lay it out to match horizontally.
            // Top and bottom and already handled in the logic above
            rect.left += parentInsets.systemWindowInsetLeft
            rect.right -= parentInsets.systemWindowInsetRight
        }

        val overlap = getOverlapPixelsForOffsetAccessor(header)

        child.layout(rect.left, rect.top - overlap, rect.right, rect.bottom - overlap)
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        offsetChildAsNeeded(parent, child, dependency)
        return false
    }

    override fun onRequestChildRectangleOnScreen(parent: CoordinatorLayout, child: View,
            rectangle: Rect, immediate: Boolean): Boolean {
        val header = parent.getDependencies(child).first()
        // Offset the rect by the child's left/top
        rectangle.offset(child.left, child.top)

        val parentRect = tempRect1
        parentRect.set(0, 0, parent.width, parent.height)

        if (!parentRect.contains(rectangle)) {
            // If the rectangle can not be fully seen the visible bounds, collapse
            // the AppBarLayout
//            header.setExpanded(false, !immediate)
            return true
        }
        return false
    }

    private fun offsetChildAsNeeded(parent: CoordinatorLayout, child: View, dependency: View) {
        val behavior = (dependency.layoutParams as CoordinatorLayout.LayoutParams).behavior as? HeaderBehavior ?: return
        // Offset the child, pinning it to the bottom the header-dependency, maintaining
        // any vertical gap and overlap

        ViewCompat.offsetTopAndBottom(child, (dependency.contentBottom - child.top
                + behavior.offsetDelta)
                - getOverlapPixelsForOffsetAccessor(dependency))
    }

    override fun getOverlapRatioForOffset(header: View): Float {
        val totalScrollRange = header.totalScrollRange
        val preScrollDown = header.downNestedPreScrollRange
        val offset = getAppBarLayoutOffset(header)

        if (preScrollDown != 0 && totalScrollRange + offset <= preScrollDown) {
            // If we're in a pre-scroll down. Don't use the offset at all.
            return 0f
        } else {
            val availScrollRange = totalScrollRange - preScrollDown
            if (availScrollRange != 0) {
                // Else we'll use a interpolated ratio of the overlap, depending on offset
                return 1f + offset / availScrollRange.toFloat()
            }
        }
        return 0f
    }

    private fun getAppBarLayoutOffset(abl: View): Int {
        val lp = abl.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = lp.behavior as? HeaderBehavior ?: return 0
        return behavior.topBottomOffsetForScrollingSibling
    }

    override fun findFirstDependency(views: List<View>): View? {
        return views.firstOrNull { it.id == R.id.profileHeader }
    }

    override fun getScrollRange(v: View): Int {
        if (v.id == R.id.profileHeader) return v.totalScrollRange
        return super.getScrollRange(v)
    }

    private val View.contentBottom
        get() = bottom - toolbarTabs.height - (parent as View).toolbar.height
}